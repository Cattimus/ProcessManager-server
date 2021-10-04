import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) {
		try {
			ManagedProcess test = new ManagedProcess("proc/main");

			test.io.write("Test");

			TimeUnit.MILLISECONDS.sleep(250);

			if(test.io.readBufferSize() > 0) {
				System.out.println(test.io.readLine());
			}

			test.stop();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Program execution is finished.");
	}
}
