import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.io.*;

import org.json.JSONArray;
import org.json.JSONObject;

/* DATA STORAGE FILE STRUCTURE

Main object:
	master: User
	users: list(User)

User:
	username: string
	passhash: hash,salt

 */

public class Auth {
	private static class User {
		public String username;
		public String passhash;
	}

	private String FilePath;

	private User master = new User();
	private final List<User> users = new ArrayList<>();

	Auth(String file) {
		FilePath = file;

		File check = new File(file);
		if(check.exists()) {
			readData();
		} else {
			master.username = "";
			master.passhash = "";
		}
	}

	public void setMasterUsername(String username) {
		master.username = username;
		writeFile();
	}

	public void setMasterPass(String password) {
		master.passhash = toBase64(hashPassword(password.toCharArray()));
		writeFile();
	}

	//change master username (requires authentication)
	public void changeMasterUsername(String username, String password, String newUsername) {
		if(checkMaster(username, password)) {
			master.username = username;
			writeFile();
		}
	}

	//change master password(requires authentication)
	public void changeMasterPass(String username, String password, String newPass) {
		if(checkMaster(username, password)) {
			master.passhash = toBase64(hashPassword(newPass.toCharArray()));
			writeFile();
		}
	}

	//change a password for a user(requires authentication)
	public void changePass(String username, String currentPass, String newPass) {
		var user = getUser(username);
		if(user == null) {
			return;
		}

		if(checkPassword(username, currentPass)) {
			user.passhash = toBase64(hashPassword(newPass.toCharArray()));
			writeFile();
		}
	}

	//change username for a user(requires authentication)
	public void changeUsername(String username, String currentPass, String newUsername) {
		var user = getUser(username);
		if(user == null) {
			return;
		}

		if(checkPassword(username, currentPass)) {
			user.username = newUsername;
			writeFile();
		}
	}

	//adds a hash to the storage file returns true on success, false on collision
	public boolean addUser(String username, String password) {
		if(!hasUser(username)) {
			User current = new User();
			current.username = username;
			current.passhash = toBase64(hashPassword(password.toCharArray()));
			users.add(current);
			writeFile();
			return true;
		}

		return false;
	}

	//remove a user (requires master authentication)
	public void delUser(String username, String masterUsername, String masterPassword) {
		var user = getUser(username);
		if(user == null) {
			return;
		}

		if(checkMaster(masterUsername, masterPassword)) {
			users.remove(user);
			writeFile();
		}
	}

	//master password challenge
	public boolean checkMaster(String username, String password) {
		var data = fromBase64(master.passhash);
		boolean result = checkHash(password.toCharArray(), data[0], data[1]);

		if(username.equals(master.username)) {
			return result;
		}

		return false;
	}

	//turn bytes to base64 encoded string
	private String toBase64(byte[][] passhash) {
		String hash = Base64.getEncoder().encodeToString(passhash[0]);
		String salt = Base64.getEncoder().encodeToString(passhash[1]);
		return (hash + "," + salt);
	}

	//return base64 encoded string to bytes
	private byte[][] fromBase64(String passhash) {
		var raw = passhash.split(",");
		byte[] hash = Base64.getDecoder().decode(raw[0]);
		byte[] salt = Base64.getDecoder().decode(raw[1]);

		byte[][] toReturn = new byte[2][];
		toReturn[0] = hash;
		toReturn[1] = salt;
		return toReturn;
	}

	//find a user based on username
	private User getUser(String username) {
		for(var user : users) {
			if(user.username.equals(username)) {
				return user;
			}
		}

		return null;
	}

	//helper function to check if database already contains user
	private boolean hasUser(String username) {
		for(var user: users) {
			if(user.username.equals(username)) {
				return true;
			}
		}
		return false;
	}

	//this will overwrite the previous file on every new addition
	private void writeFile() {
		var toWrite = toJSON();

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(FilePath));
			out.write(toWrite.toString());
			out.close();
		} catch(IOException e) {
			System.err.println("[MASTER]: Failed to write to auth file");
		}
	}

	//read data from file into a JSON object
	private JSONObject readFile() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(FilePath));
			String line = in.readLine();
			var tokens = new JSONObject(line);
			in.close();
			return tokens;
		} catch(IOException e) {
			System.err.println("[MASTER]: Unable to read auth file");
		}
		return null;
	}

	//check if the entered password matches hash on file
	public boolean checkPassword(String username, String password) {
		var user = getUser(username);
		if(user == null) {
			return false;
		}
		var data = fromBase64(user.passhash);
		return checkHash(password.toCharArray(), data[0], data[1]);
	}

	//generate a password hash and salt value (for storage and later comparison)
	private byte[][] hashPassword(char[] password) {
		SecureRandom rand = new SecureRandom();
		byte[] salt = new byte[32];
		rand.nextBytes(salt);

		//PBKDF2 with HMAC sha256 310,000 iterations (standard practice at the time of writing)
		PBEKeySpec keySpec = new PBEKeySpec(password, salt, 310000, 256);
		byte[] hash;
		SecretKeyFactory secret;

		//generate password hash
		try {
			secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			hash = secret.generateSecret(keySpec).getEncoded();
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			return null;
		}

		//return results in the format (hash,salt)
		byte[][] toReturn = new byte[2][];
		toReturn[0] = hash;
		toReturn[1] = salt;
		return toReturn;
	}

	private boolean checkHash(char[] password, byte[] passhash, byte[] salt) {
		//PBKDF2 with HMAC sha256 310,000 iterations (standard practice at the time of writing)
		PBEKeySpec keySpec = new PBEKeySpec(password, salt, 310000, 256);
		byte[] hash;
		SecretKeyFactory secret;

		//generate password hash
		try {
			secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			hash = secret.generateSecret(keySpec).getEncoded();
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			return false;
		}

		//check if password hashes match (hopefully mitigating timing attacks)
		return MessageDigest.isEqual(hash, passhash);
	}

	//deserialize from file
	private void readData() {
		var data = readFile();
		master.username = data.getString("master-username");
		master.passhash = data.getString("master-passhash");

		var users = data.getJSONArray("users");
		for(int i = 0; i < users.length(); i++) {
			var user = users.getJSONObject(i);

			User current = new User();
			current.username = user.getString("username");
			current.passhash = user.getString("passhash");

			//add user to the overall array
			this.users.add(current);
		}
	}

	//this function is exposed for debugging purposes
	public JSONObject toJSON() {
		JSONObject toWrite = new JSONObject();
		toWrite.put("master-username", master.username);
		toWrite.put("master-passhash", master.passhash);

		//add all users to array
		JSONArray users = new JSONArray();
		for(var user: this.users) {
			JSONObject current = new JSONObject();
			current.put("username", user.username);
			current.put("passhash", user.passhash);

			users.put(current);
		}

		toWrite.put("users", users);

		return toWrite;
	}
}
