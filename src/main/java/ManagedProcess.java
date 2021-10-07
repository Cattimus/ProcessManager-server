import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ManagedProcess {
	private String managerName;
	public  IOManager io = null;
	private Process proc = null;
	private ProcessLogger log;

	private final List<String> processArgs = new ArrayList<>();
	private final Map<String, String> userSignals = new HashMap<>();

	private boolean running     = false;
	private boolean autoRestart = false;
	private boolean logging     = false;

	//TODO - offer full logging history to clients on connect
	//TODO - scheduling system
	//TODO - scheduled run
	//TODO - scheduled signals
	//TODO - scheduled restart (needs to utilize user-defined signals) custom stop/start?
	//TODO - scheduled stop (needs to utilize user-defined signals)

	ManagedProcess(String managerName, String procName) {
		this.managerName = managerName;
		processArgs.add(procName);
		log = new ProcessLogger(managerName);
	}

	ManagedProcess(String managerName, String procName, String... procArgs) {
		this.managerName = managerName;
		processArgs.add(procName);
		processArgs.addAll(Arrays.asList(procArgs));
		log = new ProcessLogger(managerName);
	}

	//define a signal that may be sent to the process
	public void addSignal(String name, String signal) {
		if(!userSignals.containsKey(name)) {
			userSignals.put(name, signal);
		} else {
			System.err.println("ManagedProcess: signal " + '"' + name + '"' + " will not be added as it is already defined.");
		}
	}

	//execute an existing signal with optional arguments
	public void sendSignal(String name, String... args) {
		if(userSignals.containsKey(name)) {
			String rawSignal = userSignals.get(name);
			io.write(rawSignal);
		}
	}

	//monitor process's running status from a separate thread
	public void statusThread() {
		while(running) {

			//run logging frequently if required
			if(logging) {
				while(io.hasErr()) {
					log.addErr(io.readErr());
				}

				while(io.hasOut()) {
					log.addMsg(io.readOut());
				}
			}

			//program has crashed or been killed
			if(!proc.isAlive()) {
				running = false;

				if(autoRestart) {
					start();
				}

			//Program is running
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

				//create and start process
				ProcessBuilder temp = new ProcessBuilder(processArgs);
				running = true;
				proc = temp.start();

				//create IO manager for process
				io = new IOManager(proc.getOutputStream(), proc.getInputStream(), proc.getErrorStream());

				//monitor process
				new Thread(this::statusThread).start();

			} catch (IOException e) {
				System.err.println("Unable to start process: " + processArgs.get(0) + ".");
				e.printStackTrace();
				running = false;
			}
		} else {
			//Program has been started already (we don't want to double-start a process)
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
	public void disableTimestamp() {
		log.disableTimestamp();
	}
	public void enableTimestamp() {
		log.enableTimestamp();
	}
	public void enableLogfile() {
		if(!logging) {
			logging = true;
		}

		log.enableLogfile();
	}
	public void enableLogfile(String path) {
		if(!logging) {
			logging = true;
		}

		log.enableLogfile(path);
	}
	public void disableLogfile() {
		log.disableLogFile();
	}
}
