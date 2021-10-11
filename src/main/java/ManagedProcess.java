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
	private Thread schedulingThread;

	private boolean running     	= false;
	private boolean autoRestart 	= false;
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

	//TODO - task list must be synchronized to avoid cross-thread conflicts
	//add a new task to the task list
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
				log.addMsg("TASK", "'" + elapsed.getName() + "' has activated.");
				elapsed.reset();

				//remove if one-time task
				if (!elapsed.isEnabled()) {
					tasks.remove(elapsed);
				} else {
					log.addMsg("TASK", "'" + elapsed.getName() + "' has been reset.");
				}
			}
		}

		//if no tasks are pending, thread will exit
		scheduleRunning = false;
	}
	//helper function for scheduling thread
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
	//helper function for scheduling thread
	private ScheduledTask getElapsed() {
		for(var task : tasks) {
			if(task.isEnabled() && task.isElapsed()) {
				return task;
			}
		}

		//no elapsed task could be found
		return null;
	}

	//TODO - status thread needs to be informed if the process is killed/restarted
	//monitor process's running status from a separate thread
	private void statusThread() {
		while(running) {
			while (io.hasErr()) {
				log.addMsg("STDERR", io.readErr());
			}
			while (io.hasOut()) {
				log.addMsg("STDOUT", io.readOut());
			}

			//program has crashed or been killed
			if (!proc.isAlive()) {
				log.addMsg("Process has exited unexpectedly.");
				running = false;

				if (autoRestart) {
					start();
				}

			} else {
				//Program is running but no action is required
				try {
					TimeUnit.MILLISECONDS.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//default stop process (unsafe, no saving)
	public void stop() {
		if(running) {
			log.addMsg("Process is stopping.");
			io.destroy();
			proc.destroy();
			running = false;
			scheduleRunning = false;
			schedulingThread.interrupt();
		} else {
			log.addMsg("ERROR", "Process: " + processArgs.get(0) + " is not running and will not be stopped.");
		}
	}

	//default start process
	public void start() {
		if(!running) {
			try {
				log.addMsg("Process is starting.");
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
				log.addMsg("ERROR", "Unable to start process: " + processArgs.get(0) + ".");
				e.printStackTrace();
				running = false;
			}
		} else {
			//Program has been started already (we don't want to double-start a process)
			log.addMsg("ERROR", "Process: " + processArgs.get(0) + " is already running and will not be started.");
		}
	}

	//default restart process (unsafe)
	public void restart() {
		if(running) {
			stop();
		}
		start();
	}

	/* FORMAT
	   "<proc>", [ManagedProcessName],  [arguments], [logfiledir], [autorestart] [tasks], "</proc>"
	 */
	public void serialize(List<String> record) {
		record.add("<proc>");
		record.add(managerName);
		record.addAll(processArgs);
		record.add(log.getDir());
		record.add(Boolean.toString(autoRestart));

		for(var task : tasks) {
			task.serialize(record);
		}

		record.add("</proc>");
	}

	//process getter/setters
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

	//logging getter/setters
	public void disableTimestamp() {
		log.disableTimestamp();
	}
	public void enableTimestamp() {
		log.enableTimestamp();
	}
	public void enableLogfile() {
		log.enableLogfile();
	}
	public void enableLogfile(String path) {
		log.enableLogfile(path);
	}
	public void disableLogfile() {
		log.disableLogFile();
	}
}
