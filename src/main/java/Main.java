import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		ProcessSignal test = new ProcessSignal("--send %1 --no-recv %2 --export %3 %4");

		String result = test.send("127.0.0.1", "192.168.0.1", "[EXPORT VALUE]", "--no-orange");
		System.out.println(result);
	}
}
