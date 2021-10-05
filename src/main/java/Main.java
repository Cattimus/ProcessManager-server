import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		ManagedProcess test = new ManagedProcess("proc/somecheck");
		test.start();

		while(test.isRunning()) {
			TimeUnit.MILLISECONDS.sleep(1);
		}

		while(test.io.hasErr()) {
			System.err.println(test.io.readErr());
		}

		while(test.io.hasOut()) {
			System.out.println(test.io.readOut());
		}
	}
}
