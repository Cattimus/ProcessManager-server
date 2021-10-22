import java.io.*;

//TODO - Error messages must be reworked in this class, valid paths are sometimes classified as an error

public class ProcIO {
	private final OutputStream out;
	private final BufferedReader in;
	private final BufferedReader err;

	//starts IO threads and assigns default values
	ProcIO(OutputStream outstream, InputStream instream, InputStream errstream) {
		out = outstream;
		in = new BufferedReader(new InputStreamReader(instream));
		err = new BufferedReader(new InputStreamReader(errstream));
	}

	//write message to process stdin
	public synchronized void write(String data) {
		try {
			out.write((data + "\n").getBytes());
			out.flush();
		} catch(IOException e) {
			//This will happen if the IO channel is closed, log messages will just be clutter
		}
	}
	public synchronized void write(byte[] data) {
		try {
			out.write((new String(data) + "\n").getBytes());
			out.flush();
		} catch(IOException e) {
			//This will happen if the IO channel is closed, log messages will just be clutter
		}
	}
	public synchronized void write(char[] data) {
		try {
			out.write((new String(data) + "\n").getBytes());
			out.flush();
		} catch(IOException e) {
			//This will happen if the IO channel is closed, log messages will just be clutter
		}
	}

	//read line from process stdout
	public synchronized String readOut() {
		String line = null;

		try {
			line = in.readLine();
		} catch(IOException e) {
			//This will happen if the IO channel is closed, log messages will just be clutter
		}

		return line;
	}

	//check if stdout has data
	public synchronized boolean hasOut() {
		boolean data = false;
		try {
			data = in.ready();
		} catch(IOException e) {
			//This will happen if the IO channel is closed, log messages will just be clutter
		}

		return data;
	}

	//read line from stderr
	public synchronized String readErr() {
		String line = null;

		try {
			line = err.readLine();
		} catch(IOException e) {
			//This will happen if the IO channel is closed, log messages will just be clutter
		}

		return line;
	}

	//check if stderr has data
	public synchronized boolean hasErr() {
		boolean data = false;
		try {
			data = err.ready();
		} catch(IOException e) {
			//This will happen if the IO channel is closed, log messages will just be clutter
		}

		return data;
	}

	//close output to stdin
	public synchronized void closeOut() {
		try {
			out.close();
		} catch(IOException e) {
			//this happens when the program has already exited
		}
	}

	//close input from stdout
	public synchronized void closeIn() {
		try {
			in.close();
		} catch(IOException e) {
			//this happens when the program has already exited
		}
	}

	//close input from stderr
	public synchronized void closeErr() {
		try {
			err.close();
		} catch(IOException e) {
			//this happens when the program has already exited
		}
	}

	//close all outputs and inputs
	public synchronized void destroy() {
		closeOut();
		closeIn();
		closeErr();
	}
}
