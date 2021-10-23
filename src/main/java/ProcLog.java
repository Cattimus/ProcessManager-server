import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Deque;
import java.util.LinkedList;

public class ProcLog {
	private String logFilePath;
	private final String managerID;
	private BufferedWriter logOut = null;

	private boolean logfile = false;
	private boolean timestamp = true;
	private boolean stdout = false;

	private final Deque<String> logCache = new LinkedList<String>();
	private int cacheLimit = 1024;

	ProcLog(String managerName) {
		logFilePath = managerName + ".log";
		managerID = managerName;
	}

	//TODO - dump cache to file efficiently instead of writing one at a time
	//TODO - [URGENT] need a more graceful method of identifying different sources of log output
	//TODO - priority logging messages, Different priority levels will be written to different sources but all will be saved in history

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

	//add message to the cache and automatically remove expired messages
	private void cache(String msg) {
		logCache.addLast(msg);

		if(logCache.size() > cacheLimit) {
			logCache.removeFirst();
		}

		//logfiles and stdout are updated upon new message cached
		if(logfile) {
			try {
				logOut.write(msg + "\n");
				logOut.flush();
			} catch(IOException e) {
				System.err.println(managerID + ": unable to write to logfile.");
			}
		}
		if(stdout) {
			System.out.println(msg);
		}
	}

	//print message queue
	public void printCache() {
		System.out.println("Cache size: " + logCache.size());

		for(var msg : logCache) {
			System.out.println(msg);
		}

		logCache.clear();
	}

	//return the current iteration of the cache for sending to clients
	public String[] getCache() {
		return logCache.toArray(new String[logCache.size()]);
	}

	//add log entry (stdout)
	public void addMsg(String msg) {
		String currentTime = "";
		if(timestamp) {
			currentTime = (LocalDateTime.now()).format(DateTimeFormatter.ofPattern("MM-dd-yy HH:mm:ss.SS - "));
		}

		cache(currentTime + "[" + managerID + "]: " + msg);
	}

	public void addMsg(String info, String msg) {
		String currentTime = "";
		if(timestamp) {
			currentTime = (LocalDateTime.now()).format(DateTimeFormatter.ofPattern("MM-dd-yy HH:mm:ss.SS - "));
		}

		cache(currentTime + "[" + managerID + "][" + info + "]: " + msg);
	}

	public void enableTimestamp() {
		timestamp = true;
	}
	public void disableTimestamp() {
		timestamp = false;
	}
	public String getDir() {
		return logFilePath;
	}
	public void enableStdout() {
		stdout = true;
	}
	public void disableStdout() {
		stdout = false;
	}
	public int getCacheLimit() {
		return cacheLimit;
	}
	public void setCacheLimit(int limit) {
		cacheLimit = limit;

		//to prevent odd behavior from negative caching limits
		if(cacheLimit < 1024) {
			cacheLimit = 1024;
		}
	}
}
