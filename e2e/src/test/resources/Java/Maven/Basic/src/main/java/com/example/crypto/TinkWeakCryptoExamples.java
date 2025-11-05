package com.example.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.Mac;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.mac.MacFactory;
import com.google.crypto.tink.mac.MacKeyTemplates;
import com.google.crypto.tink.proto.KeyTemplate;
import java.security.GeneralSecurityException;

/**
 * This class contains intentional Google Tink weak cryptographic patterns
 * for testing the SonarCrypto plugin. DO NOT use this code in production!
 */
public class TinkWeakCryptoExamples {

    /**
     * Uses weak AES-128 key size with Tink - VULNERABILITY
     * Should be flagged by WeakTinkPatterns.crysl rule
     */
    public Aead createWeakAesGcmAead() throws GeneralSecurityException {
        // Using 128-bit AES key - should be flagged as weak (< 256 bits)
        KeyTemplate weakTemplate = AeadKeyTemplates.createAesGcmKeyTemplate(16); // 16 bytes = 128 bits
        KeysetHandle keysetHandle = KeysetHandle.generateNew(weakTemplate);
        return AeadFactory.getPrimitive(keysetHandle);
    }

    /**
     * Uses weak AES-EAX configuration - VULNERABILITY
     * Should be flagged by WeakTinkPatterns.crysl rule
     */
    public Aead createWeakAesEaxAead() throws GeneralSecurityException {
        // Using 128-bit AES key with small IV - should be flagged as weak
        KeyTemplate weakTemplate = AeadKeyTemplates.createAesEaxKeyTemplate(16, 8); // 16 byte key, 8 byte IV
        KeysetHandle keysetHandle = KeysetHandle.generateNew(weakTemplate);
        return AeadFactory.getPrimitive(keysetHandle);
    }

    /**
     * Uses weak HMAC configuration - VULNERABILITY
     * Should be flagged by WeakTinkPatterns.crysl rule
     */
    public Mac createWeakHmacMac() throws GeneralSecurityException {
        // Using small key size and weak tag size - should be flagged
        KeyTemplate weakTemplate = MacKeyTemplates.createHmacKeyTemplate(16, 8, com.google.crypto.tink.proto.HashType.SHA256); // 16 byte key, 8 byte tag
        KeysetHandle keysetHandle = KeysetHandle.generateNew(weakTemplate);
        return MacFactory.getPrimitive(keysetHandle);
    }

    /**
     * Uses multiple weak Tink patterns together - VULNERABILITY
     */
    public void demonstrateMultipleWeakPatterns() throws GeneralSecurityException {
        // Create multiple weak crypto objects
        Aead weakAead1 = createWeakAesGcmAead();
        Aead weakAead2 = createWeakAesEaxAead();
        Mac weakMac = createWeakHmacMac();
        
        // Use them in a way that should trigger violations
        byte[] plaintext = "sensitive data".getBytes();
        byte[] ciphertext = weakAead1.encrypt(plaintext, null);
        
        byte[] data = "data to authenticate".getBytes();
        byte[] tag = weakMac.computeMac(data);
        
        // This demonstrates the usage of weak crypto primitives
        System.out.println("Encrypted with weak AES: " + ciphertext.length + " bytes");
        System.out.println("MAC with weak config: " + tag.length + " bytes");
    }

    /**
     * Uses deprecated or potentially unsafe Tink patterns - VULNERABILITY
     */
    public void useUnsafeTinkPatterns() throws GeneralSecurityException {
        // Example of using very small parameters that should be flagged
        
        // Extremely small key size (should definitely be flagged)
        try {
            KeyTemplate veryWeakTemplate = AeadKeyTemplates.createAesGcmKeyTemplate(8); // 64 bits - very weak
            KeysetHandle keysetHandle = KeysetHandle.generateNew(veryWeakTemplate);
            Aead veryWeakAead = AeadFactory.getPrimitive(keysetHandle);
            
            byte[] data = "test".getBytes();
            byte[] encrypted = veryWeakAead.encrypt(data, null);
            System.out.println("Very weak encryption performed: " + encrypted.length);
        } catch (Exception e) {
            System.out.println("Very weak encryption failed (expected): " + e.getMessage());
        }
    }

    /**
     * Demonstrates hardcoded key material with Tink - VULNERABILITY
     * While Tink handles key generation, this shows poor key management
     */
    public void demonstrateWeakKeyManagement() throws GeneralSecurityException {
        // This is not directly a Tink weak crypto issue, but shows poor practices
        // that could be detected by extended rules
        
        byte[] hardcodedKeyMaterial = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10
        }; // 128-bit hardcoded key - VULNERABILITY
        
        System.out.println("Using hardcoded key material: " + hardcodedKeyMaterial.length + " bytes");
        
        // In a real scenario, this would be used to create a keyset improperly
        // Tink recommends using KeysetHandle.generateNew() instead
    }

    /**
     * Main method to run all weak crypto examples
     */
    public static void main(String[] args) {
        TinkWeakCryptoExamples examples = new TinkWeakCryptoExamples();
        
        try {
            System.out.println("=== Tink Weak Crypto Examples ===");
            examples.demonstrateMultipleWeakPatterns();
            examples.useUnsafeTinkPatterns();
            examples.demonstrateWeakKeyManagement();
            System.out.println("=== All examples completed ===");
        } catch (Exception e) {
            System.err.println("Error running examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
}