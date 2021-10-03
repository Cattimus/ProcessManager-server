import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException{
		Runtime rt = Runtime.getRuntime();
		Process test = rt.exec("proc/main");

		InputStream in = test.getInputStream();
		OutputStream out = test.getOutputStream();

		int counter = 0;
		boolean running = true;
		//main loop while program is running
		while(running) {
			TimeUnit.MILLISECONDS.sleep(50);

			//write input to program and exit gracefully
			try {
				if (counter == 10) {
					out.write("quit\n".getBytes());
				} else {
					out.write((counter + "\n").getBytes());
				}
				out.flush();
			} catch(Exception e) {
				running = false;
			}

			counter++;
		}

		//read from program stdout
		byte[] results = in.readNBytes(8192);
		System.out.println(new String(results));

		System.out.println("Program execution is finished.");
	}
}
