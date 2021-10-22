import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.*;

//TODO - clients must be able to reschedule existing tasks
//TODO - offer full logging history to clients on connect

public class Proc {
	private String managerName;
	private ProcIO io = null;
	private Process proc = null;
	private final ProcLog log;

	private final List<String> processArgs  = new ArrayList<>();
	private final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
	private Thread schedulingThread;
	private Thread monitorThread;

	private boolean running     	= false;
	private boolean autoRestart 	= false;
	private boolean scheduleRunning = false;

	Proc(String managerName, String procName) {
		this.managerName = managerName;
		processArgs.add(procName);
		log = new ProcLog(managerName);
	}

	Proc(String managerName, String procName, String... procArgs) {
		this.managerName = managerName;
		processArgs.add(procName);
		processArgs.addAll(Arrays.asList(procArgs));
		log = new ProcLog(managerName);
	}

	//add a new task to the task list
	public void addTask(Task task) {
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

	//send signal directly to process (without having to build a scheduledtask
	public void sendSignal(String signal) {
		io.write(signal);
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

			Task elapsed = getElapsed();
			if (elapsed != null) {
				log.addMsg("TASK", "'" + elapsed.getName() + "' has activated.");
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
	private Task getElapsed() {
		for(var task : tasks) {
			if(task.isEnabled() && task.isElapsed()) {
				return task;
			}
		}

		//no elapsed task could be found
		return null;
	}

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
				log.addMsg("Process has exited.");
				stop();

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
	public synchronized void stop() {
		if(running) {
			io.destroy();
			proc.destroy();
			running = false;
			scheduleRunning = false;

			if(schedulingThread != null) {
				schedulingThread.interrupt();
			}
		}
	}

	//default start process
	public synchronized void start() {
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
				io = new ProcIO(proc.getOutputStream(), proc.getInputStream(), proc.getErrorStream());

				//monitor process
				monitorThread = new Thread(this::statusThread);
				monitorThread.start();

			} catch (IOException e) {
				log.addMsg("ERROR", "Unable to start process: " + processArgs.get(0) + ".");
				e.printStackTrace();
				running = false;
			}
		}
	}

	//default restart process (unsafe)
	public synchronized void restart() {
		if(running) {
			stop();
		}
		start();
	}

	/* FORMAT
	   type: process
	   name: managerName
	   args: processArgs(array of String)
	   logging-dir: log.dir
	   auto-restart: autoRestart(boolean)
	   tasks: tasks(array of ScheduledTask)
	 */
	public JSONObject serialize() {
		JSONObject record = new JSONObject();
		record.put("type", "process");
		record.put("name", managerName);
		JSONArray args = new JSONArray();
		args.putAll(processArgs);
		record.put("args", processArgs);
		record.put("logging-dir", log.getDir());
		record.put("auto-restart", autoRestart);

		JSONArray taskList = new JSONArray();

		for(var task : tasks) {
			taskList.put(task.serialize());
		}
		record.put("tasks", taskList);

		return record;
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
