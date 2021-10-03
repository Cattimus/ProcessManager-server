import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class IOManager {

	//output to process stdin
	private final OutputStream out;
	private final BlockingQueue<String> outputLines = new LinkedBlockingQueue<>();
	private boolean outputOpen;

	//input from process stdout
	private final InputStream in;
	private final BlockingQueue<String> inputLines = new LinkedBlockingQueue<>();
	private boolean inputOpen;

	//starts IO threads and assigns default values
	IOManager(OutputStream outstream, InputStream instream) {
		out = outstream;
		outputOpen = true;
		new Thread(this::outputLoop).start();

		in = instream;
		inputOpen = true;
		new Thread(this::inputLoop).start();
	}

	//manage writing to program
	private void outputLoop() {
		while(outputOpen) {
			try {
				if (outputLines.size() > 0) {
					out.write(outputLines.remove().getBytes());
					out.flush();
				} else {
					TimeUnit.MILLISECONDS.sleep(50);
				}
			} catch(IOException e) {
				//TODO - Need a more graceful method of exiting after a broken pipe
				outputOpen = false;
				break;
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	//manage reading from program
	private void inputLoop() {
		while(inputOpen) {
			try {
				String data = new String(in.readAllBytes());
				inputLines.add(data);
			} catch(IOException e) {
				//TODO - Need a more graceful method of exiting after a broken pipe
				inputOpen = false;
				break;
			}
		}
	}

	//add message to output queue (newline is added since that is expected by most programs for user input)
	public void write(String data) {
		outputLines.add((data + "\n"));
	}
	public void write(byte[] data) {
		outputLines.add((new String(data) + "\n"));
	}
	public void write(char[] data) {
		outputLines.add((new String(data) + "\n"));
	}

	//read line from output feed if available
	public String readLine() {
		return inputLines.remove();
	}

	//show how many lines are currently available
	public int inputCount() {
		return inputLines.size();
	}

	//gracefully close pipe
	public void closeOutput() {
		outputOpen = false;

		try {
			out.close();
		} catch(IOException e) {
			//this happens when the program has already exited
		}
	}

	public void closeInput() {
		inputOpen = false;

		try {
			in.close();
		} catch(IOException e) {
			//this happens when the program has already exited
		}
	}

	public void destroy() {
		closeOutput();
		closeInput();
	}

	public boolean outputActive() {
		return outputOpen;
	}

	public boolean inputActive() {
		return inputOpen;
	}
}
