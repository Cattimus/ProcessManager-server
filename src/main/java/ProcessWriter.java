import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ProcessWriter implements Runnable {
	private Thread t;
	private OutputStream out;
	private BlockingQueue<byte[]> messageList = new LinkedBlockingQueue<>();
	private boolean streamOpen;

	//assigns values and starts thread
	ProcessWriter(OutputStream stream) {
		out = stream;
		streamOpen = true;
		t = new Thread(this);
		t.start();
	}

	//Loop of thread
	public void run() {
		while(streamOpen) {
			try {
				if (messageList.size() > 0) {
					out.write(messageList.remove());
					out.flush();
				} else {
					TimeUnit.MILLISECONDS.sleep(50);
				}
			} catch(IOException e) {
				//TODO - Need a more graceful method of exiting after a broken pipe
				streamOpen = false;
				break;
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	//add message to output queue
	public void write(String data) {
		messageList.add(data.getBytes());
	}
	public void write(byte[] data) {
		messageList.add(data);
	}
	public void write(char[] data) {
		messageList.add(data.toString().getBytes());
	}

	//gracefully close pipe
	public void close() {
		streamOpen = false;

		try {
			out.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	//check if thread is running (from main thread)
	public boolean active() {
		return streamOpen;
	}
}
