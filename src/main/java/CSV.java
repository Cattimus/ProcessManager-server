import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* CSV Implementation general rules:
   -Commas inside of variables are escaped with \ (\,)
   -Any non-escaped comma will be considered a separator
   -All non-separator values are considered strings and will be deserialized as such by default
 */

//helper class to handle working with CSV data
public class CSV {
	private String filePath;
	private List<List<String>> data = new ArrayList<>();

	//read file and fill out data
	CSV(String filePath) {
		this.filePath = filePath;
		File testFile = new File(filePath);

		//file doesnt exist (create empty CSV)
		if(!testFile.exists()) {
			return;
		}

		//open file
		try {
			BufferedReader in = new BufferedReader(new FileReader(filePath));

			String line = "";
			while(line != null) {
				line = in.readLine();

				if(line != null) {
					//separate only on un-escaped commas
					var fields = line.split("(?<!\\\\),");

					//replace escaped separators
					for(var field : fields) {
						field.replace("\\,", ",");
					}

					//add the line of our csv file to the collection
					data.add(Arrays.asList(fields));
				}

			}

			in.close();
		} catch(IOException e) {
			System.err.println("CSV unable to open file: " + filePath);
		}
	}

	public void writeFile() {
		File testFile = new File(filePath);

		if(!testFile.exists()) {
			try {
				testFile.createNewFile();
			}catch(IOException e) {
				System.err.println("CSV failed to create new file: " + filePath);
				return;
			}
		}

		try {
			//NOTE - this will destroy any existing data in the file
			BufferedWriter out = new BufferedWriter(new FileWriter(filePath));

			for(var line: data) {
				//create new CSV line
				String outline = String.join(",", line);

				//write line to file
				out.write(outline + "\n");
				out.flush();
			}

			//close file
			out.close();
		}catch(IOException e) {
			System.err.println("CSV failed to write to file: " + filePath);
		}
	}

	//add new record
	public void addRecord(List<String> record) {
		data.add(record);
	}

	//create a new record
	public void newRecord() {
		data.add(new ArrayList<>());
	}

	//get a copy of an existing record
	public List<String> getRecord(int recordIndex) {
		return data.get(recordIndex);
	}

	public int recordCount() {
		return data.size();
	}


	//support for CSV serialization of project types
	public static class Serializer {

	}
}
