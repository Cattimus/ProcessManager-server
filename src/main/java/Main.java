import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) {
		ManagedProcess test = new ManagedProcess("proc/main");

		test.io.write("Test");

		try {
			TimeUnit.MILLISECONDS.sleep(250);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		if(test.io.readBufferSize() > 0) {
			System.out.println(test.io.readLine());
		}
		test.stop();

		System.out.println("Program execution is finished.");
	}
}
