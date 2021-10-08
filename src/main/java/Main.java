import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		ManagedProcess test = new ManagedProcess("test", "python3", "proc/main.py");
		test.addTask(ScheduledTask.Builder.newInstance("Graceful shutdown")
				.sendSignal("quit gracefully")
				.once()
				.at(LocalTime.of(19,28,30))
				.build());
		test.start();

		while(test.isRunning()) {
			TimeUnit.MILLISECONDS.sleep(1);
		}
	}
}
