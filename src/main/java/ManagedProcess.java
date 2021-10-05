import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ManagedProcess {
	public IOManager io = null;
	private Process proc = null;
	private boolean running = false;
	private final List<String> processArgs = new ArrayList<>();
	private final Map<String, ProcessSignal> userSignals = new HashMap<>();

	//TODO - scheduled run
	//TODO - scheduled restart (needs to utilize user-defined signals) custom stop/start?
	//TODO - scheduled stop (needs to utilize user-defined signals)
	//TODO - auto-restart (on crash or exit)

	ManagedProcess(String procName) {
		processArgs.add(procName);

		new Thread(this::aliveCheck).start();
	}

	ManagedProcess(String procName, String... procArgs) {
		processArgs.add(procName);
		processArgs.addAll(Arrays.asList(procArgs));

		new Thread(this::aliveCheck).start();
	}

	public void addSignal(String name, ProcessSignal signal) {
		if(!userSignals.containsKey(name)) {
			userSignals.put(name, signal);
		} else {
			System.err.println("ManagedProcess: signal " + '"' + name + '"' + " will not be added as it is already defined.");
		}
	}

	public void sendSignal(String name, String... args) {
		if(userSignals.containsKey(name)) {
			String rawSignal = userSignals.get(name).send(args);

			if(rawSignal != null) {
				io.write(rawSignal);
			}
		}
	}

	//monitor process's running status from a separate thread
	public void aliveCheck() {
		while(running) {
			if(!proc.isAlive()) {
				running = false;
				io.destroy();
			} else {
				try {
					TimeUnit.MILLISECONDS.sleep(50);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//default stop process
	public void stop() {
		if(running) {
			io.destroy();
			proc.destroy();
			running = false;
		} else {
			System.err.println("Process: " + processArgs.get(0) + " is not running and will not be stopped.");
		}
	}

	//default start process
	public void start() {
		if(!running) {
			try {
				ProcessBuilder temp = new ProcessBuilder(processArgs);
				proc = temp.start();
				io = new IOManager(proc.getOutputStream(), proc.getInputStream());
				running = true;
				new Thread(this::aliveCheck).start();
			} catch (IOException e) {
				System.err.println("Unable to start process: " + processArgs.get(0) + ".");
				e.printStackTrace();
				running = false;
			}
		} else {
			System.err.println("Process: " + processArgs.get(0) + " is already running and will not be started.");
		}
	}

	//default restart process
	public void restart() {
		if(running) {
			stop();
		}
		start();
	}

	public long getPID() {
		return proc.pid();
	}

	public boolean isRunning() {
		return running;
	}
}
