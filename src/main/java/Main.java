import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		ProcessSignal stop = new ProcessSignal("quit gracefully");
		ProcessSignal save = new ProcessSignal("save-all");
		ProcessSignal send = new ProcessSignal("--send %1");

		ManagedProcess test = new ManagedProcess("python3", "proc/main.py");
		test.addSignal("stop", stop);
		test.addSignal("save", save);
		test.addSignal("send", send);
		test.enableAutorestart();

		test.start();
		test.io.write("Test");
		TimeUnit.MILLISECONDS.sleep(250);
		test.sendSignal("save");
		TimeUnit.MILLISECONDS.sleep(250);
		test.sendSignal("send", "Test");
		TimeUnit.MILLISECONDS.sleep(250);

		while(test.io.readBufferSize() > 0) {
			System.out.println(test.io.readLine());
		}

		test.sendSignal("stop");
		TimeUnit.MILLISECONDS.sleep(250);

		while(test.io.readBufferSize() > 0) {
			System.out.println(test.io.readLine());
		}

		test.disableAutorestart();

		test.sendSignal("stop");
		TimeUnit.MILLISECONDS.sleep(250);

		while(test.io.readBufferSize() > 0) {
			System.out.println(test.io.readLine());
		}
	}
}
