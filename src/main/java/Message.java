import org.json.JSONObject;

/*
valid operations:
request/response

valid types:
Process
Task
System

Process and Task will be set to a JSONObject of thier respective classes
System will have a custom type
 */

public class Message {
	public String operation;
	public String type;
	public int count = 0;
	public int token = 0;
	public JSONObject data = null;

	Message(String msg) {
		JSONObject info = new JSONObject(msg);

		operation = info.getString("operation");
		type = info.getString("type");
		count = info.getInt("object count");
		token = info.getInt("token");
		data = info.getJSONObject("data");
	}
}
