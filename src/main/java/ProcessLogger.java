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

	//TODO - [URGENT] need a more graceful method of identifying different sources of log output
	//TODO - remote logging
	//TODO - log history without writing to file

	//sets up the logger for writing to a file instead of stdout
	public void enableLogfile() {
		if(logfile) {
			return;
		}
		//check if file already exists
		File checkFile = new File(logFilePath);
		try {
			if(!checkFile.exists()) {
				//if file does not exist, create a new file
				if(!checkFile.createNewFile()) {
					//file creation fails
					System.err.println(managerID + ": unable to create logfile. File logging disabled.");
					logfile = false;
					return;
				}
			}
			//open a writer to the file
			logOut = new BufferedWriter(new FileWriter(checkFile, true));
		} catch(IOException e) {
			System.err.println(managerID + ": unable to create logfile. File logging disabled.");
			logfile = false;
			return;
		}
		logfile = true;
	}

	//enable log file with a string
	public void enableLogfile(String newPath) {
		if(logfile) {
			return;
		}

		//reset old log file path and assign new log file path
		logFilePath = managerID + ".log";
		logFilePath = newPath + logFilePath;
		enableLogfile();
	}

	//close output stream to file and disable file logging
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

	//additional function to close the output file stream, as it may not be clear
	public void destroy() {
		if(logOut != null) {
			disableLogFile();
		}
	}

	//add log entry (stdout)
	public void addMsg(String msg) {
		String currentTime = "";
		if(timestamp) {
			currentTime = (LocalDateTime.now()).format(DateTimeFormatter.ofPattern("MM-dd-yy HH:mm:ss.SS - "));
		}

		if(logfile) {
			try {
				logOut.write(currentTime + msg + "\n");
				logOut.flush();
			} catch(IOException e) {
				System.err.println(managerID + ": unable to write to logfile.");
			}
		} else {
			System.out.println(currentTime + "[" + managerID + "]: " + msg);
		}
	}

	public void addMsg(String info, String msg) {
		String currentTime = "";
		if(timestamp) {
			currentTime = (LocalDateTime.now()).format(DateTimeFormatter.ofPattern("MM-dd-yy HH:mm:ss.SS - "));
		}

		if(logfile) {
			try {
				logOut.write(currentTime + "[" + managerID + "][" + info + "]: " + msg + "\n");
				logOut.flush();
			} catch(IOException e) {
				System.err.println(managerID + ": unable to write to logfile.");
			}
		} else {
			System.out.println(currentTime + "[" + managerID + "][" + info + "]: " + msg);
		}
	}

	void enableTimestamp() {
		timestamp = true;
	}
	void disableTimestamp() {
		timestamp = false;
	}
}
