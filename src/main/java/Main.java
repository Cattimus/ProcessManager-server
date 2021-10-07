import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		/*
		ManagedProcess test = new ManagedProcess("Biggus testus", "proc/somecheck");
		test.enableLogfile("log/");
		test.start();

		while(test.isRunning()) {
			TimeUnit.MILLISECONDS.sleep(1);
		}*/

		ScheduledTask test = ScheduledTask.Builder.newInstance()
				.startProcess()
				.daily().
				at(LocalTime.of(20, 32))
				.build();

		while(!test.isElapsed()) {
			TimeUnit.MILLISECONDS.sleep(50);
		}

		System.out.println("timer has elapsed at: " + LocalDateTime.now());

		test.reset();
	}
}
