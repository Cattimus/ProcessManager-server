import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException{
		ManagedProcess test = new ManagedProcess("proc/main");
		test.start();

		test.io.write("Ree");

		TimeUnit.SECONDS.sleep(1);

		if(test.io.inputCount() > 0) {
			System.out.println(test.io.readLine());
		}


		test.stop();
	}
}
