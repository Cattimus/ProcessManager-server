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
	master-username: string
	master-passhash: hash,salt
	users: list(JSON object)

User:
	username: string
	PATs: array(full of PAT hashes) PAT,salt

 */

//TODO - need a secure method of distributing PAT, probably handled over TLS for client later
//TODO - handle null cases for reading from files
//TODO - clear PATs for a user (will assign a new PAT)

public class AuthenticationManager {
	private class User {
		public String username;
		public List<String> PATs = new ArrayList<>();
	}

	private String FilePath;
	private String masterUsername;
	private String masterPasshash;
	private List<User> users = new ArrayList<>();

	AuthenticationManager(String file) {
		FilePath = file;

		File check = new File(file);
		if(check.exists()) {
			readData();
		} else {
			masterUsername = "";
			masterPasshash = "";
		}
	}

	public void setMasterUsername(String username) {
		masterUsername = username;
	}

	public void setMasterPass(String password) {
		masterPasshash = toBase64(hashPassword(password.toCharArray()));
	}

	//master password challenge
	public boolean checkMaster(String username, String password) {
		var data = fromBase64(masterPasshash);
		boolean result = checkHash(password.toCharArray(), data[0], data[1]);

		if(username.equals(masterUsername)) {
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

	//helper function to check if database already contains user
	public boolean hasUser(String username) {
		for(var user: users) {
			if(user.username.equals(username)) {
				return true;
			}
		}
		return false;
	}

	//adds a hash to the storage file returns true on success, false on collision
	public boolean addUser(String username, String password) {
		if(!hasUser(username)) {
			User current = new User();
			current.username = username;
			current.PATs.add(toBase64(hashPassword(password.toCharArray())));
			users.add(current);
			writeFile();
			return true;
		}

		return false;
	}

	//add new PAT to existing user
	public boolean addPAT(String username, String PAT) {
		for(var user : users) {
			if(user.username.equals(username)) {
				user.PATs.add(toBase64(hashPassword(PAT.toCharArray())));
				writeFile();
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
		for(var user : users) {

			//username has a match
			if(user.username.equals(username)) {

				//check all recorded PATs
				for(var pass : user.PATs) {
					var data = fromBase64(pass);

					//PAT and password match
					boolean result = checkHash(password.toCharArray(), data[0], data[1]);
					if(result) {
						return true;
					}
				}
			}
		}

		//no matches found for user
		return false;
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

	//generate a PAT (personal access token) that will allow a user to connect in lieu of a password
	//this will be passed to the standard hashPassword and checkPassword functions, only the hash will be stored long-term
	//generating and/or adding new PATs to the file will require authentication with the master password
	public static char[] genPAT() {
		SecureRandom rand = new SecureRandom();

		//generate an alphabet of accepted characters (all valid normal and special characters in ASCII but space)
		StringBuilder alphabet = new StringBuilder();
		for(char i = '!'; i <= '~'; i++) {
			alphabet.append(i);
		}
		char[] charSource = alphabet.toString().toCharArray();

		//generate a new 32 character PAT given the alphabet
		char[] PAT = new char[32];
		for(int i = 0; i < 32; i++) {
			PAT[i] = charSource[rand.nextInt(charSource.length)];
		}

		return PAT;
	}

	//deserialize from file
	private void readData() {
		var data = readFile();
		masterUsername = data.getString("master-username");
		masterPasshash = data.getString("master-passhash");

		var users = data.getJSONArray("users");
		for(int i = 0; i < users.length(); i++) {
			var user = users.getJSONObject(i);

			User current = new User();
			current.username = user.getString("username");

			//add user PATs
			var PATs = user.getJSONArray("PATs");
			for(int x = 0; x < PATs.length(); x++) {
				current.PATs.add(PATs.getString(x));
			}

			//add user to the overall array
			this.users.add(current);
		}
	}

	//this function is exposed for debugging purposes
	public JSONObject toJSON() {
		JSONObject toWrite = new JSONObject();
		toWrite.put("master-username", masterUsername);
		toWrite.put("master-passhash", masterPasshash);

		//add all users to array
		JSONArray users = new JSONArray();
		for(var user: this.users) {
			JSONObject current = new JSONObject();
			current.put("username", user.username);

			JSONArray PATs = new JSONArray();
			PATs.putAll(user.PATs);
			current.put("PATs", PATs);

			users.put(current);
		}

		toWrite.put("users", users);

		return toWrite;
	}
}
