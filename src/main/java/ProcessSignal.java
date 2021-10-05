import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessSignal {
	private List<String>   staticArgs    = new ArrayList<>();
	private List<Integer>  escapeIndexes = new ArrayList<>();
	int argCount = 0;

	/* The signal is defined by a format string and optional arguments.
	*  Arguments may be defined in the command string by use of
	*  %-esacped number values, these values will proceed in numerical order.
	*
	*  For example: "--file %1 --exclude %2 --with %1"
	*  defines a command string with 2 arguments. %1 and %2.
	*  A repeated escaped character will repeat the argument. */
	ProcessSignal(String signalFormat) {
		parseFormat(signalFormat);
	}

	//parse a format string into static arguments and escape indexes
	private void parseFormat(String signalFormat) {
		var staticMatches = signalFormat.split("(?<!\\\\)%\\d+");
		Pattern pattern = Pattern.compile("(?<!\\\\)%\\d+");
		Matcher match = pattern.matcher(signalFormat);

		for(var i : staticMatches) {
			staticArgs.add(i);
		}
		while(match.find()) {
			escapeIndexes.add(Integer.parseInt(match.group().replace("%", "")));
		}

		for(var i : escapeIndexes) {
			if(i > argCount) {
				 argCount = i;
			}
		}
	}

	//returns a formatted argument string if the correct number of arguments are given
	//returns null if invalid arguments are provided
	public String send(String... args) {
		if(args.length < argCount) {
			return null;
		}

		String toReturn = "";

		for(int i = 0; i < argCount; i++) {
			toReturn += staticArgs.get(i);
			toReturn += args[escapeIndexes.get(i) - 1];
		}

		if(staticArgs.size() > argCount) {
			toReturn += staticArgs.get(staticArgs.size() - 1);
		}

		return toReturn;
	}

	public int argCount() {
		return argCount;
	}

	public void showStatic() {
		for(var i : staticArgs) {
			System.out.println(i);
		}
	}

	public void showArgs () {
		for(var i : escapeIndexes) {
			System.out.println(i);
		}
	}
}
