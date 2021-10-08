import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class AuthenticationManager {
	private String filePath;
	private boolean masterEnabled;

	AuthenticationManager(){}

	//generate a password hash and salt value (for storage and later comparison)
	public byte[][] hashPassword(String password) {
		SecureRandom rand = new SecureRandom();
		byte[] salt = new byte[32];
		rand.nextBytes(salt);

		//PBKDF2 with HMAC sha256 310,000 iterations (standard practice at the time of writing)
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 310000, 256);
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

	public boolean checkPassword(char[] password, byte[] passhash, byte[] salt) {
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
}
