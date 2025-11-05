package com.example.crypto;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;

/**
 * This class contains intentional BouncyCastle cryptographic vulnerabilities
 * for testing the SonarCrypto plugin. DO NOT use this code in production!
 */
public class BouncyCastleVulnerabilities {

    static {
        // Add BouncyCastle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    // Hard-coded key - VULNERABILITY
    private static final byte[] HARDCODED_KEY = "MyVerySecretKey!".getBytes();
    
    // Weak IV - VULNERABILITY
    private static final byte[] WEAK_IV = {0, 0, 0, 0, 0, 0, 0, 0};

    /**
     * Uses DES with BouncyCastle - VULNERABILITY
     */
    public byte[] encryptWithBouncyCastleDES(byte[] plaintext) throws Exception {
        // DES is weak and deprecated
        DESEngine desEngine = new DESEngine();
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(desEngine));
        
        // Using hard-coded key - VULNERABILITY
        KeyParameter keyParam = new KeyParameter(HARDCODED_KEY, 0, 8); // DES uses 8 bytes
        
        // Using weak IV - VULNERABILITY
        ParametersWithIV params = new ParametersWithIV(keyParam, WEAK_IV);
        cipher.init(true, params);
        
        byte[] output = new byte[cipher.getOutputSize(plaintext.length)];
        int len = cipher.processBytes(plaintext, 0, plaintext.length, output, 0);
        cipher.doFinal(output, len);
        
        return output;
    }

    /**
     * Uses RC4 stream cipher - VULNERABILITY
     */
    public byte[] encryptWithRC4(byte[] plaintext) {
        // RC4 is considered insecure
        RC4Engine rc4 = new RC4Engine();
        
        // Using hard-coded key - VULNERABILITY
        KeyParameter keyParam = new KeyParameter(HARDCODED_KEY);
        rc4.init(true, keyParam);
        
        byte[] ciphertext = new byte[plaintext.length];
        rc4.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);
        
        return ciphertext;
    }

    /**
     * Uses Blowfish with small key - VULNERABILITY
     */
    public byte[] encryptWithWeakBlowfish(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("Blowfish", "BC");
        
        // Using very short key - VULNERABILITY
        byte[] shortKey = "key".getBytes(); // Only 3 bytes - too short
        SecretKeySpec keySpec = new SecretKeySpec(shortKey, "Blowfish");
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    /**
     * Uses IDEA cipher without proper key management - VULNERABILITY
     */
    public byte[] encryptWithIDEA(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("IDEA", "BC");
        
        // Hard-coded key - VULNERABILITY
        byte[] ideaKey = "0123456789abcdef".getBytes(); // 16 bytes for IDEA
        SecretKeySpec keySpec = new SecretKeySpec(ideaKey, "IDEA");
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    /**
     * Uses deprecated cipher suite - VULNERABILITY
     */
    public byte[] encryptWithDeprecatedCipher(byte[] data) throws Exception {
        // Using old cipher transformation that may be vulnerable
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding", "BC");
        
        // Hard-coded 3DES key - VULNERABILITY
        byte[] tripleDesKey = "MySecretKeyForTripleDES123!".getBytes();
        SecretKeySpec keySpec = new SecretKeySpec(tripleDesKey, 0, 24, "DESede");
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    /**
     * Uses weak parameters with AES - VULNERABILITY
     */
    public byte[] encryptAESWithWeakParams(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "BC");
        
        // Using predictable key - VULNERABILITY
        byte[] predictableKey = new byte[16];
        for (int i = 0; i < 16; i++) {
            predictableKey[i] = (byte) i; // Sequential bytes - predictable
        }
        
        SecretKeySpec keySpec = new SecretKeySpec(predictableKey, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        
        return cipher.doFinal(data);
    }

    /**
     * Unsafe key derivation - VULNERABILITY
     */
    public byte[] deriveKeyUnsafely(String password, String salt) {
        // Simple concatenation instead of proper key derivation - VULNERABILITY
        String combined = password + salt;
        return combined.getBytes(); // No proper key stretching
    }

    /**
     * Uses NULL encryption with BouncyCastle - VULNERABILITY
     */
    public byte[] encryptWithNull(byte[] data) throws Exception {
        // NULL cipher - provides no security
        Cipher cipher = Cipher.getInstance("NULL", "BC");
        return data; // Returns plaintext
    }

    /**
     * Exposes BouncyCastle internal state - VULNERABILITY
     */
    public void exposeInternalState() {
        // Exposing sensitive cryptographic state - VULNERABILITY
        System.out.println("Hard-coded key: " + new String(HARDCODED_KEY));
        System.out.println("Weak IV: " + java.util.Arrays.toString(WEAK_IV));
        
        // Logging provider information that might leak sensitive data
        System.out.println("BC Provider: " + Security.getProvider("BC").getInfo());
    }
}