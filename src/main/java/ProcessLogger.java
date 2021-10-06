import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProcessLogger {
	private String logFilePath;
	private final String managerID;
	private BufferedWriter logOut = null;

	private boolean logfile = false;
	private boolean timestamp = true;

	ProcessLogger(String managerName) {
		logFilePath = managerName + ".log";
		managerID = managerName;
	}

	//TODO - remote logging
	//TODO - log history without writing to file

	public void enableLogfile() {
		if(logfile) {
			return;
		}

		File checkFile = new File(logFilePath);

		try {
			if(!checkFile.exists()) {
				if(!checkFile.createNewFile()) {
					System.err.println(managerID + ": unable to create logfile. File logging disabled.");
					logfile = false;
					return;
				}
			}
			logOut = new BufferedWriter(new FileWriter(checkFile, true));
		} catch(IOException e) {
			System.err.println(managerID + ": unable to create logfile. File logging disabled.");
			logfile = false;
			return;
		}

		logfile = true;
	}

	public void enableLogfile(String newPath) {
		if(logfile) {
			return;
		}

		logFilePath = newPath + logFilePath;
		enableLogfile();
	}

	public void disableLogFile() {
		if(!logfile) {
			return;
		}

		logfile = false;

		if(logOut != null) {
			try {
				logOut.close();
			} catch (IOException e) {
				//this is called when the file is already closed
			}
		}
	}

	public void destroy() {
		if(logOut != null) {
			disableLogFile();
		}
	}

	//add log entry (stdout)
	public void add(String msg) {
		try {
			if(timestamp) {
				String currentTime = (LocalDateTime.now()).format(DateTimeFormatter.ofPattern("MM-dd-yy hh:mm:ss.SS - "));
				if(logfile) {
					logOut.write(currentTime + msg + "\n");
					logOut.flush();
				} else {
					System.out.println(currentTime + "[" + managerID + "]: " + msg);
				}
			} else {
				if(logfile) {
					logOut.write(msg + "\n");
					logOut.flush();
				} else {
					System.out.println("[" + managerID + "]: " + msg);
				}
			}
		} catch(IOException e) {
			System.err.println(managerID + ": unable to write to logfile.");
		}
	}

	//add log entry(error)
	public void addErr(String msg) {
		try {
			if(timestamp) {
				//dash is left out so errors can be spotted at a glance in logfiles
				String currentTime = (LocalDateTime.now()).format(DateTimeFormatter.ofPattern("MM-dd-yy hh:mm:ss.SS "));
				if(logfile) {
					logOut.write(currentTime + "[ERROR]: " + msg + "\n");
					logOut.flush();
				} else {
					System.err.println(currentTime + "[ERROR][" + managerID + "]: " + msg);
				}
			} else {
				if(logfile) {
					logOut.write("[ERROR]: " + msg + "\n");
					logOut.flush();
				} else {
					System.err.println("[ERROR][" + managerID + "]: " + msg);
				}
			}
		} catch(IOException e) {
			System.err.println(managerID + ": unable to write to logfile.");
		}
	}

	void enableTimestamp() {
		timestamp = true;
	}

	void disableTimestamp() {
		timestamp = false;
	}
}
