import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/* DATA STORAGE FILE STRUCTURE
   Serialization has been decided to be conducted using CSV file format, as the external dependency of JSON is undesirable.
   The Format of stored password hashes will be the following:
   master password will be stored under the reserved username "master" - to be implemented later

   [username][hash][salt]
   username, hash, salt

   Additional PAT hashes and salts may be stored on the same line, they will simply be appended to the line.
 */

//TODO - check if username already exists in database before adding new user
//TODO - need a secure method of distributing PAT, probably handled over TLS for client later

public class AuthenticationManager {
	CSV tokens;

	AuthenticationManager(String file){
		tokens = new CSV(file);
	}

	//helper function to check if database already contains user
	public boolean hasUser(String username) {
		for(int i = 0; i < tokens.recordCount(); i++) {
			if(tokens.getRecord(i).get(i).equals(username)) {
				return true;
			}
		}
		return false;
	}

	//adds a hash to the storage file returns true on success, false on collision
	public boolean addUser(String username, byte[] hash, byte[] salt) {

		if(!hasUser(username)) {
			List<String> toAdd = new ArrayList<>();
			toAdd.add(username);
			toAdd.add(Base64.getEncoder().encodeToString(hash));
			toAdd.add(Base64.getEncoder().encodeToString(salt));
			tokens.addRecord(toAdd);
			tokens.writeFile();
			return true;
		}

		return false;
	}

	//check if the entered password matches hash on file
	public boolean checkPassword(String username, String password) {
		for(int i = 0; i < tokens.recordCount(); i++) {
			var currentRecord = tokens.getRecord(i);
			if(currentRecord.get(0).equals(username)) {
				return checkPassword(
						password.toCharArray(),
						Base64.getDecoder().decode(currentRecord.get(1)),
						Base64.getDecoder().decode(currentRecord.get(2))
				);
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
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
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
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
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
}
