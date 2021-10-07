import java.time.LocalDate;
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
				at(LocalTime.of(17, 0))
				.build();

		test.printTime();
		test.setElapseTime(LocalDateTime.now().plusHours(2));
		test.printTime();
		test.setElapseTime(LocalTime.of(15, 0));
		test.printTime();
		test.setElapseTime(LocalDateTime.of(2021,10, 7, 17, 0));
		test.printTime();
	}
}
