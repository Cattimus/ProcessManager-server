import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/* TEMPORARY FILE STRUCTURE
   In the future, this will most likely be handled by a JSON file. However, as a temporary measure
   this will be handled in a custom file format which will be described here.

   The first line of this file will contain the master password hash (base64 encoded) followed by , and
   the salt value used (also base 64 encoded) followed by a newline.

   Subsequent lines will contain PAT hashes (base 64 encoded) followed by , and the salt value used during hashing,
   of course also base 64 encoded. The number of recognized PAT tokens depends on this file for the short term.

   In the future, it may be worthwhile to explore encrypting this file along with the global config file with
   the master password for increased security. However, this may prove impractical. As such, this program should not be
   exposed to any unsafe or potentially unsafe network.
 */

//TODO - need a secure method of distributing PAT, probably handled over TLS for client later

public class AuthenticationManager {
	private String filePath;
	BufferedWriter out;
	BufferedReader in;

	AuthenticationManager(String file){
		filePath = file;
		File credentials = new File(filePath);
		try {
			if (!credentials.exists()) {
				credentials.createNewFile();
			}

			out = new BufferedWriter(new FileWriter(filePath, true));
			in = new BufferedReader(new FileReader(filePath));
		} catch(IOException e) {
			System.err.println("Unable to create or open credentials file. Program must exit.");
			System.exit(1);
		}
	}

	//TODO - update to JSON serialization
	//adds a hash to the storage file
	public void addUser(String username, byte[] hash, byte[] salt) {
		try {
			//structure is username,passwordhash,salt
			out.write(username + "," + Base64.getEncoder().encodeToString(hash) + "," + Base64.getEncoder().encodeToString(salt) + "\n");
			out.flush();
		} catch (IOException e) {
			System.out.println("Unable to access credentials file. Cannot add new hash");
			System.exit(1);
		}
	}
	public boolean checkPassword(String username, String password) {
		try {
			in.reset();
		} catch(IOException e) {
			//This will happen if the file does not need to be reset
		}

		String line = "";
		while(line != null) {
			try {
				line = in.readLine();
			} catch (IOException e) {
				System.err.println("Unable to read credentials file.");
				System.exit(1);
				break;
			}

			if(line != null) {
				var data = line.split(",");

				if(data[0].equals(username)) {
					return checkPassword(password.toCharArray(), Base64.getDecoder().decode(data[1]), Base64.getDecoder().decode(data[2]));
				}
			}

		}

		return false;
	}

	//generate a password hash and salt value (for storage and later comparison)
	public byte[][] hashPassword(char[] password) {
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
		} catch (InvalidKeySpecException e) {
			return null;
		} catch(NoSuchAlgorithmException e) {
			return null;
		}

		//return results in the format (hash,salt)
		byte[][] toReturn = new byte[2][];
		toReturn[0] = hash;
		toReturn[1] = salt;
		return toReturn;
	}

	private boolean checkPassword(char[] password, byte[] passhash, byte[] salt) {
		//PBKDF2 with HMAC sha256 310,000 iterations (standard practice at the time of writing)
		PBEKeySpec keySpec = new PBEKeySpec(password, salt, 310000, 256);
		byte[] hash;
		SecretKeyFactory secret;

		//generate password hash
		try {
			secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			hash = secret.generateSecret(keySpec).getEncoded();
		} catch (InvalidKeySpecException e) {
			return false;
		} catch(NoSuchAlgorithmException e) {
			return false;
		}

		//check if password hashes match (hopefully mitigating timing attacks)
		return MessageDigest.isEqual(hash, passhash);
	}

	//generate a PAT (personal access token) that will allow a user to connect in lieu of a password
	//this will be passed to the standard hashPassword and checkPassword functions, only the hash will be stored long-term
	//generating and/or adding new PATs to the file will require authentication with the master password
	public char[] genPAT() {
		SecureRandom rand = new SecureRandom();
		byte[] rawPAT = new byte[32];
		rand.nextBytes(rawPAT);

		return Base64.getEncoder().encodeToString(rawPAT).toCharArray();
	}

	public void destroy() {
		try {
			out.close();
			in.close();
		} catch(IOException e) {
			//This doesn't matter because they're already closed
		}
	}
}
