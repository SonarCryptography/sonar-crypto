package org.sonarcrypto.test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws Exception {
		final var key = KeyGenerator.getInstance("AES").generateKey();
		
		final var algorithm = "AES/CBC/PKCS5Padding";
		//final var algorithm = "AES/GCM/NoPadding";
		final var cipher = Cipher.getInstance(algorithm);
		//cipher.init(Cipher.ENCRYPT_MODE, key);
		final var messageBytes = "message".getBytes();
		cipher.doFinal(messageBytes, 0, messageBytes.length);
	}
}
