import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManagedProcess {
	public IOManager io = null;
	private Process proc = null;
	private boolean running = false;
	private final List<String> processArgs;

	//TODO - sanitize input before arbitrarily executing processes
	ManagedProcess(String procName) {
		processArgs = new ArrayList<>();
		processArgs.add(procName);

		start();
	}

	ManagedProcess(String procName, String[] procArgs) {
		processArgs = new ArrayList<>();
		processArgs.add(procName);
		processArgs.addAll(Arrays.asList(procArgs));

		start();
	}

	public void stop() {
		if(running) {
			io.destroy();
			proc.destroy();
			proc = null;
			io = null;
			running = false;
		} else {
			System.err.println("Process: " + processArgs.get(0) + " is not running and will not be stopped.");
		}
	}

	public void start() {
		if(!running) {
			try {
				ProcessBuilder temp = new ProcessBuilder(processArgs);
				proc = temp.start();
				io = new IOManager(proc.getOutputStream(), proc.getInputStream());
				running = true;
			} catch (IOException e) {
				System.err.println("Unable to start process: " + processArgs.get(0) + ".");
				e.printStackTrace();
				running = false;
			}
		} else {
			System.err.println("Process: " + processArgs.get(0) + " is already running and will not be started.");
		}
	}

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
