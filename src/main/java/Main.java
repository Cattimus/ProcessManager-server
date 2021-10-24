import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

public class Main {

	public static class State {
		public static String credentialsFile; //file where password hashes are stored
		public static String logDirectory;    //directory where logs are written (if enabled)
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Listening for clients on socket 31243...");
		ServerSocket server = new ServerSocket(31243);
		var temp = server.accept();
		System.out.println("New connection has been accepted on: " + temp.getLocalSocketAddress());

		Client test = new Client(temp);
		test.write("Yep it worked.\n");
	}
}
