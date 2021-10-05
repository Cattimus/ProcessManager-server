import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		ProcessSignal stop = new ProcessSignal("quit gracefully");
		ProcessSignal start = new ProcessSignal("\"arg goes here\"");

		ManagedProcess test = new ManagedProcess("python3", "proc/main.py");
		test.addSignal("start", start);
		test.addSignal("stop", stop);

		test.start();
		test.io.write("Test");

		TimeUnit.MILLISECONDS.sleep(250);

		while(test.io.readBufferSize() > 0) {
			System.out.println(test.io.readLine());
		}

		test.sendSignal("stop");
		TimeUnit.MILLISECONDS.sleep(250);

		while(test.io.readBufferSize() > 0) {
			System.out.println(test.io.readLine());
		}
	}
}
