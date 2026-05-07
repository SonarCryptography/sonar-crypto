package com.example.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class contains intentional cryptographic vulnerabilities for testing
 * the SonarCrypto plugin. DO NOT use this code in production!
 */
public class WeakCryptoExamples {

    // Hard-coded key - VULNERABILITY
    private static final String HARDCODED_KEY = "MySecretKey12345";

    // Hard-coded password - VULNERABILITY
    private static final String HARDCODED_PASSWORD = "admin123";

    /**
     * Uses weak DES encryption algorithm - VULNERABILITY
     */
    public byte[] encryptWithDES(byte[] data) throws Exception {
        // DES is considered weak and insecure
        Cipher cipher = Cipher.getInstance("DES"); // CC: ALGORITHM/InvalidValue "DES"

        // Using hard-coded key - VULNERABILITY
        SecretKeySpec keySpec = new SecretKeySpec(HARDCODED_KEY.getBytes(), "DES"); // CC: ALGORITHM/InvalidValue "DES", KEY_MATERIAL/ImproperGenerated, KEY_MATERIAL/ForbiddenType "java.lang.String"
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        return cipher.doFinal(data);
    }

    /**
     * Uses MD5 hashing algorithm - VULNERABILITY
     */
    public String hashWithMD5(String input) throws NoSuchAlgorithmException {
        // MD5 is cryptographically broken
        MessageDigest md = MessageDigest.getInstance("MD5"); // CC: ALGORITHM/InvalidValue "MD5"
        byte[] hashBytes = md.digest(input.getBytes());

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Uses SHA1 hashing algorithm - VULNERABILITY
     */
    public String hashWithSHA1(String input) throws NoSuchAlgorithmException {
        // SHA1 is considered weak
        MessageDigest md = MessageDigest.getInstance("SHA1"); // CC: ALGORITHM/InvalidValue "SHA1"
        byte[] hashBytes = md.digest(input.getBytes());

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // FIXME: Does not find anything, because `Random` is not used in a cryptographic context.
    ///**
    // * Uses insecure random number generator - VULNERABILITY
    // */
    //public byte[] generateWeakRandom() {
    //    // Using java.util.Random instead of SecureRandom - VULNERABILITY
    //    Random random = new Random();
    //    byte[] randomBytes = new byte[16];
    //    random.nextBytes(randomBytes);
    //    return randomBytes;
    //}

    /**
     * Uses predictable seed for SecureRandom - VULNERABILITY
     */
    public byte[] generatePredictableRandom() {
        // Using predictable seed - VULNERABILITY
        SecureRandom random = new SecureRandom();
        random.setSeed(12345); // Fixed seed makes it predictable; CC: KEY_MATERIAL/ImproperGenerated

        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * Uses weak key size for AES - VULNERABILITY
     */
    public SecretKey generateWeakAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        // 64-bit key might be considered weak for some applications
        keyGen.init(64); // CC: KEY_MATERIAL "64"
        return keyGen.generateKey();
    }

    /**
     * Uses ECB mode without padding - VULNERABILITY
     */
    public byte[] encryptWithECB(byte[] data) throws Exception {
        // ECB mode is insecure - identical plaintext blocks produce identical ciphertext
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding"); // CC: MODE "ECB", KEY_MATERIAL/ForbiddenType "java.lang.String", KEY_MATERIAL/ImproperGenerated

        SecretKeySpec keySpec = new SecretKeySpec(HARDCODED_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        return cipher.doFinal(data);
    }

    /**
     * Missing initialization vector for CBC mode - VULNERABILITY
     */
    public byte[] encryptWithoutIV(byte[] data) throws Exception {
        // CBC mode without explicit IV - may use predictable IV
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // CC: MODE "CBC"

        SecretKeySpec keySpec = new SecretKeySpec(HARDCODED_KEY.getBytes(), "AES"); // CC: KEY_MATERIAL/ForbiddenType "java.lang.String", KEY_MATERIAL/ImproperGenerated
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        return cipher.doFinal(data);
    }

    /**
     * Uses null cipher - VULNERABILITY
     */
    public byte[] encryptWithNullCipher(byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException {
        // NULL cipher provides no encryption
        Cipher cipher = Cipher.getInstance("NULL"); // CC: ALGORITHM/InvalidValue "NULL", API_MISUSE/IncompleteOperation "javax.crypto.Cipher"
        return data; // Returns data unencrypted
    }

    // FIXME: Does not find anything. Does CrySL support this use case?
    ///**
    // * Exposes sensitive data in logs - VULNERABILITY
    // */
    //public void logSensitiveData(String username, String password) {
    //    // Logging sensitive information - VULNERABILITY
    //    System.out.println("User credentials: " + username + ":" + password);
    //    System.out.println("Hardcoded key: " + HARDCODED_KEY);
    //}
}