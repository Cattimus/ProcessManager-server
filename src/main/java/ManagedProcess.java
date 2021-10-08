import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ManagedProcess {
	private String managerName;
	public  IOManager io = null;
	private Process proc = null;
	private ProcessLogger log;

	private final List<String> processArgs        = new ArrayList<>();
	private final List<ScheduledTask> tasks       = new ArrayList<>();
	private final Map<String, String> userSignals = new HashMap<>();

	private Thread schedulingThread;

	private boolean running     	= false;
	private boolean autoRestart 	= false;
	private boolean logging     	= false;
	private boolean scheduleRunning = false;

	//TODO - offer full logging history to clients on connect

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

	public void addTask(ScheduledTask task) {
		tasks.add(task);
		log.addMsg("New task has been added: '" + task.getName() + "'. Set to activate at: " + task.getElapseTime());

		//interrupt scheduling thread so it can act on the task if the scheduling thread is running
		if(scheduleRunning) {
			schedulingThread.interrupt();
		} else {
			//activate scheduling thread
			scheduleRunning = true;
			schedulingThread = new Thread(this::scheduleThread);
			schedulingThread.start();
		}
	}

	//TODO - seems there's an intermittent bug with this function, sometimes returns null unexpectedly
	private LocalDateTime waitTime() {
		LocalDateTime toReturn = null;

		//find the next task to be executed
		for(var task : tasks) {
			if(task.isEnabled() && task.isEnabled()) {
				if(toReturn == null) {
					toReturn = task.getElapseTime();
				} else if(task.getElapseTime().isBefore(toReturn)) {
					toReturn = task.getElapseTime();
				}
			}
		}

		return toReturn;
	}

	private ScheduledTask getElapsed() {
		for(var task : tasks) {
			if(task.isEnabled() && task.isElapsed()) {
				return task;
			}
		}

		//no elapsed task could be found
		return null;
	}

	//logic behind scheduled events
	private void scheduleThread() {
		while(scheduleRunning && tasks.size() > 0) {
			LocalDateTime wait = waitTime();
			Duration toWait = Duration.between(LocalDateTime.now(), wait);

			//wait for the next task to be elapsed unless interrupted
			try {
				TimeUnit.MILLISECONDS.sleep(toWait.toMillis());
			} catch (InterruptedException e) {
				//new task has been added to thread
				continue;
			}

			//TODO - monitoring thread needs to be informed if we're meddling with the process
			ScheduledTask elapsed = getElapsed();
			if (elapsed != null) {
				switch (elapsed.getType()) {
					case NONE:
						break;

					case START:
						start();
						break;

					case STOP:
						stop();
						break;

					case RESTART:
						restart();
						break;

					case SIGNAL:
						io.write(elapsed.getSignal());
						break;
				}
				log.addMsg("'" + elapsed.getName() + "' has activated.");
				elapsed.reset();

				//remove if one-time task
				if (!elapsed.isEnabled()) {
					tasks.remove(elapsed);
				} else {
					log.addMsg("'" + elapsed.getName() + "' has been reset.");
				}
			}
		}

		//if no tasks are pending, thread will exit
		scheduleRunning = false;
	}

	//monitor process's running status from a separate thread
	//TODO - status thread needs to handle a process being restarted if autorestart is enabled
	private void statusThread() {
		while(running) {

			//run logging if required
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
				log.addMsg("Process has exited unexpectedly.");
				running = false;

				if(autoRestart) {
					start();
				}

			//Program is running but no action is required
			} else {
				try {
					TimeUnit.MILLISECONDS.sleep(50);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//default stop process (unsafe, no saving)
	public void stop() {
		if(running) {
			io.destroy();
			proc.destroy();
			running = false;
			scheduleRunning = false;
			schedulingThread.interrupt();
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
