import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException{
		Runtime rt = Runtime.getRuntime();
		Process test = rt.exec("proc/main");

		InputStream in = test.getInputStream();
		ProcessWriter out = new ProcessWriter(test.getOutputStream());

		int counter = 0;
		//main loop while program is running
		while(out.active()) {

			if(counter == 10) {
				out.write("quit\n");
			} else {
				out.write(counter + "\n");
			}

			TimeUnit.MILLISECONDS.sleep(50);
			counter++;
		}

		//read from program stdout
		byte[] results = in.readNBytes(8192);
		System.out.println(new String(results));

		System.out.println("Program execution is finished.");
	}
}
