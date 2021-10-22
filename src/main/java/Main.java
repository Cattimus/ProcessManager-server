import java.net.Authenticator;
import java.security.AuthProvider;
import java.time.LocalTime;
import java.util.*;
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

	public static void main(String[] args) {
		ManagedProcess test = new ManagedProcess("test", "python3", "proc/main.py");
		test.addTask(ScheduledTask.Builder.newInstance("Graceful shutdown")
				.sendSignal("quit gracefully")
				.once()
				.at(LocalTime.of(12,38))
				.build());
		test.start();

		System.out.println(test.serialize().toString());

		test.stop();

	}
}
