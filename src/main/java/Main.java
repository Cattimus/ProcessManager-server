import java.net.Authenticator;
import java.security.AuthProvider;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

public class Main {

	//project-wide requirements
	//TODO Authentication manager
	//TODO - Master password authentication/storage
	//TODO - PAT generation
	//TODO - PAT authentication/storage

	//TODO config file
	//TODO - Format unknown
	//TODO - Global static state class

	//TODO serialization
	//TODO - Will depend on org.json for JSON support
	//TODO - Each class will handle it's own serialization?
	//TODO - Main serialization class?

	//TODO Connection manager
	//TODO - Accept connection
	//TODO - Communicate with client over protocol
	//TODO - Possibly custom message class to be re-implemented clientside as a protocol
	//TODO - Immediately reject any IP which provides an invalid token
	//TODO - Obfuscation of connection protocol to discourage port scanners
	//TODO - TLS sockets (essential for security)

	public static class State {

	}

	public static void main(String[] args) throws InterruptedException {
		/*
		ManagedProcess test = new ManagedProcess("test", "python3", "proc/main.py");
		test.addTask(ScheduledTask.Builder.newInstance("Graceful shutdown")
				.sendSignal("quit gracefully")
				.once()
				.at(LocalTime.of(19,28,30))
				.build());
		test.start();

		while(test.isRunning()) {
			TimeUnit.MILLISECONDS.sleep(1);
		}*/

		AuthenticationManager test = new AuthenticationManager();
		var hash = test.hashPassword("Test");
		System.out.println(test.checkPassword("Test".toCharArray(), hash[0], hash[1]));
		System.out.println(test.checkPassword("test".toCharArray(), hash[0], hash[1]));
	}
}
