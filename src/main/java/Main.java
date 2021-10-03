import java.io.*;
import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) throws IOException, InterruptedException{
		Runtime rt = Runtime.getRuntime();
		Process test = rt.exec("proc/main");
		IOManager proctest = new IOManager(test.getOutputStream(), test.getInputStream());

		int counter = 0;
		//main loop while program is running
		while(proctest.outputActive()) {

			if(counter == 10) {
				proctest.write("quit");
			} else {
				proctest.write(Integer.toString(counter));
			}

			if(proctest.inputCount() > 0) {
				System.out.print(proctest.readLine());
			}

			TimeUnit.MILLISECONDS.sleep(50);
			counter++;
		}

		System.out.println("Program execution is finished.");
		proctest.destroy();
	}
}
