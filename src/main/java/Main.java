import java.util.concurrent.TimeUnit;

public class Main {

	//project-wide requirements
	//TODO Connection manager
	//TODO - Accept connection
	//TODO - Communicate with client over protocol
	//TODO - Possibly custom message class to be re-implemented clientside as a protocol
	//TODO - Immediately reject any IP which provides an invalid token
	//TODO - Obfuscation of connection protocol to discourage port scanners
	//TODO - TLS sockets (essential for security)

	public static class State {
		public static String credentialsFile; //file where password hashes are stored
		public static String logDirectory;    //directory where logs are written (if enabled)
	}

	public static void main(String[] args) throws InterruptedException{
		Proc test = new Proc("test", "python3", "proc/main.py");
		test.start();

		while(!test.isRunning()) {
			TimeUnit.MILLISECONDS.sleep(1);
		}

		test.sendSignal("quit gracefully");

		TimeUnit.MILLISECONDS.sleep(500);

		test.stop();

	}
}
