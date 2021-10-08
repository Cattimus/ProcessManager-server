import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		ManagedProcess test = new ManagedProcess("test", "python3", "proc/main.py");
		test.enableLogging();

		ScheduledTask exampleSignal = ScheduledTask.Builder.newInstance("graceful shutdown")
				.sendSignal("quit gracefully")
				.once()
				.at(LocalTime.of(19,11))
				.build();

		test.addTask(exampleSignal);

		test.start();

		while(test.isRunning()) {
			TimeUnit.MILLISECONDS.sleep(1);
		}
	}
}
