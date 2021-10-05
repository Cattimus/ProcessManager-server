import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		ManagedProcess test = new ManagedProcess("proc/somecheck");
		test.start();
		test.enableLogging();

		while(test.isRunning()) {
			TimeUnit.MILLISECONDS.sleep(50);
		}

	}
}
