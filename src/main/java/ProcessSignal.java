import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* NOTE: This will remain in the source for the time being as it may be reworked later on.
   However, this class is quite overkill for the utility it would provide, and ultimately
   just boils down to a string. Arguments are not particularly useful and storing arguments
   alongside the string amounts to a static string. This class will likely be removed.
 */

public class ProcessSignal {
	private final List<String>   staticArgs    = new ArrayList<>();
	private final List<Integer>  escapeIndexes = new ArrayList<>();
	private int argCount = 0;

	/* The signal is defined by a format string and optional arguments.
	*  Arguments may be defined in the command string by use of
	*  %-esacped number values, these values will proceed in numerical order.
	*
	*  For example: "--file %1 --exclude %2 --with %1"
	*  defines a command string with 2 arguments. %1 and %2.
	*  Arguments should start at 1 and increase
	*  A repeated escaped character will repeat the argument. */
	ProcessSignal(String signalFormat) {
		parseFormat(signalFormat);
	}

	//parse a format string into static arguments and escape indexes
	private void parseFormat(String signalFormat) {
		var staticMatches = signalFormat.split("(?<!\\\\)%\\d+");
		Pattern pattern = Pattern.compile("(?<!\\\\)%\\d+");
		Matcher match = pattern.matcher(signalFormat);

		staticArgs.addAll(Arrays.asList(staticMatches));
		while(match.find()) {
			int temp = Integer.parseInt(match.group().replace("%", ""));
			escapeIndexes.add(temp);

			if(temp > argCount) {
				argCount = temp;
			}
		}
	}

	//returns a formatted argument string if the correct number of arguments are given
	//returns null if invalid arguments are provided
	public String send(String... args) {
		if(args.length < argCount) {
			return null;
		}

		StringBuilder toReturn = new StringBuilder();
		for(int i = 0; i < argCount; i++) {
			toReturn.append(staticArgs.get(i));
			toReturn.append(args[escapeIndexes.get(i) - 1]);
		}
		if(staticArgs.size() > argCount) {
			toReturn.append(staticArgs.get(staticArgs.size() - 1));
		}

		return toReturn.toString();
	}

	public int argCount() {
		return argCount;
	}
}
