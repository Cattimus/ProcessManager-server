import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

//TODO Connection manager
//TODO - Communicate with client over protocol
//TODO - Possibly custom message class to be re-implemented clientside as a protocol
//TODO - Immediately reject any IP which provides an invalid token
//TODO - Obfuscation of connection protocol to discourage port scanners
//TODO - TLS sockets (essential for security)

//TODO - generate token upon connection, used to identify unique client. If a user responds with the wrong token they are disconnected immediately

public class Client {
	private Socket remote;
	OutputStream out = null;
	BufferedReader in = null;
	String username = null;
	int token = 0;

	//connection should already be accepted and authenticated from main by this point
	Client(Socket remote, String username) {
		this.remote = remote;
		this.username = username;

		try {
			out = remote.getOutputStream();
			in = new BufferedReader( new InputStreamReader(remote.getInputStream()));
		} catch(IOException e) {
			System.err.println("[MASTER]: client failed to connect properly.");
		}

		new Thread(this::listenThread).start();
	}

	//read input from socket
	public void listenThread() {
		try {
			String msg = null;
			while ((msg = in.readLine()) != null) {
				System.out.println("Input recieved from client: " + msg);
				Main.State.messageQueue.add(new Message(msg));
			}
		} catch (IOException e) {
			//force close on failure to read?
		}
	}

	//write output to socket
	public synchronized void write(String toSend) {
		try {
			out.write(toSend.getBytes(StandardCharsets.US_ASCII));
			out.flush();
		} catch(IOException e) {
			//force close on failure to write?
		}
	}
}
