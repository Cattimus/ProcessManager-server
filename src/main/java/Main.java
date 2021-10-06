import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		ManagedProcess test = new ManagedProcess("Biggus testus", "proc/somecheck");
		test.enableLogfile("log/");
		test.start();

		while(test.isRunning()) {
			TimeUnit.MILLISECONDS.sleep(1);
		}
	}
}
