import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManagedProcess {
	public IOManager io = null;
	private Process proc = null;
	private final List<String> processArgs;

	//TODO - sanitize input before arbitrarily executing processes
	ManagedProcess(String procName) {
		processArgs = new ArrayList<>();
		processArgs.add(procName);
	}

	ManagedProcess(String procName, String[] procArgs) {
		processArgs = new ArrayList<>();
		processArgs.add(procName);
		processArgs.addAll(Arrays.asList(procArgs));
	}

	public void stop() {
		io.destroy();
		proc.destroy();
		proc = null;
		io = null;
	}

	public void start() throws IOException {
		if(proc == null && io == null) {
			ProcessBuilder temp = new ProcessBuilder(processArgs);
			proc = temp.start();
			io = new IOManager(proc.getOutputStream(), proc.getInputStream());
		}
	}

	public void restart() {
		stop();
		try {
			start();
		} catch(IOException e) {
			System.err.println("restart: Unable to restart process: " + processArgs.get(0) + ".");
			e.printStackTrace();
		}
	}

	public long getPID() {
		return proc.pid();
	}
}
