import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ManagedProcess {
	public IOManager io = null;
	private Process proc = null;

	private final List<String> processArgs = new ArrayList<>();
	private final Map<String, ProcessSignal> userSignals = new HashMap<>();

	private boolean running = false;
	private boolean autoRestart = false;
	private boolean logging = false;

	//TODO - logging using files (stdout stderr as fallbacks)
	//TODO - full logging history (possibly unreasonable)
	//TODO - scheduling system
	//TODO - scheduled run
	//TODO - scheduled signals
	//TODO - scheduled restart (needs to utilize user-defined signals) custom stop/start?
	//TODO - scheduled stop (needs to utilize user-defined signals)

	ManagedProcess(String procName) {
		processArgs.add(procName);

	}

	ManagedProcess(String procName, String... procArgs) {
		processArgs.add(procName);
		processArgs.addAll(Arrays.asList(procArgs));

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
	public void statusThread() {
		while(running) {

			//run logging frequently if required
			if(logging) {
				while(io.hasErr()) {
					System.err.println(io.readErr());
				}

				while(io.hasOut()) {
					System.out.println(io.readOut());
				}
			}

			//program has crashed or been killed
			if(!proc.isAlive()) {
				running = false;

				if(autoRestart) {
					start();
				}

			//Program is idle
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
				if(io != null) {
					io.destroy();
				}

				ProcessBuilder temp = new ProcessBuilder(processArgs);
				proc = temp.start();
				io = new IOManager(proc.getOutputStream(), proc.getInputStream(), proc.getErrorStream());
				running = true;
				new Thread(this::statusThread).start();
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

	public void enableAutorestart() {
		autoRestart = true;
	}

	public void disableAutorestart() {
		autoRestart = false;
	}

	public void enableLogging() {
		logging = true;
	}

	public void disableLogging() {
		logging = false;
	}
}
