package com.example.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Test cases for javax.crypto.Mac covering valid usages, weak algorithms,
 * improper key material, typestate/sequence violations, and complex
 * interprocedural/control-flow edge cases.
 */
public class MacTests {

    private static final String HARDCODED_MAC_KEY = "SuperSecretMacKey123456789012345";
    private static final byte[] STATIC_KEY_BYTES = new byte[] {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
    };

    private SecretKey fieldValidKey;
    private SecretKey fieldHardcodedKey;

    // =========================================================================
    // SECTION 1: VALID / NORMAL CASES (NO ISSUE)
    // =========================================================================

    /**
     * 1. Valid HmacSHA256 with KeyGenerator and single-shot doFinal(byte[]) - NO ISSUE
     */
    public byte[] validHmacSha256SingleShot(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(data);
    }

    /**
     * 2. Valid HmacSHA384 with update() followed by doFinal() - NO ISSUE
     */
    public byte[] validHmacSha384UpdateAndDoFinal(byte[] part1, byte[] part2) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA384");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA384");
        mac.init(key);
        mac.update(part1);
        mac.update(part2);
        return mac.doFinal();
    }

    /**
     * 3. Valid HmacSHA512 with SecureRandom-backed SecretKeySpec - NO ISSUE
     */
    public byte[] validHmacSha512WithSecureRandomKey(byte[] data) throws GeneralSecurityException {
        byte[] keyBytes = new byte[64];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(keyBytes);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA512");
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(keySpec);
        return mac.doFinal(data);
    }

    /**
     * 4. Valid Mac with ByteBuffer update and output buffer doFinal(byte[], int) - NO ISSUE
     */
    public byte[] validHmacWithByteBufferAndOutputArray(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        mac.update(ByteBuffer.wrap(data));

        byte[] output = new byte[mac.getMacLength()];
        mac.doFinal(output, 0);
        return output;
    }

    /**
     * 5. Valid Mac with single-byte and offset/length updates - NO ISSUE
     */
    public byte[] validHmacWithOffsetUpdates(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        if (data.length > 4) {
            mac.update(data[0]);
            mac.update(data, 1, data.length - 1);
        } else {
            mac.update(data);
        }
        return mac.doFinal();
    }

    /**
     * 6. Valid Mac reuse across multiple messages after doFinal() - NO ISSUE
     */
    public byte[] validHmacReuseMultipleMessages(byte[] msg1, byte[] msg2) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] tag1 = mac.doFinal(msg1);
        mac.update(msg2);
        byte[] tag2 = mac.doFinal();
        return tag1.length == tag2.length ? tag2 : tag1;
    }

    /**
     * 7. Valid Mac with explicit reset() between updates before doFinal() - NO ISSUE
     */
    public byte[] validHmacWithReset(byte[] abortedData, byte[] realData) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        mac.update(abortedData);
        mac.reset();
        mac.update(realData);
        return mac.doFinal();
    }

    /**
     * 8. Valid Mac using helper method for key generation - NO ISSUE
     */
    public byte[] validHmacInterproceduralKey(byte[] data) throws GeneralSecurityException {
        SecretKey key = createValidHmacKey("HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(data);
    }

    private SecretKey createValidHmacKey(String algorithm) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
        return keyGen.generateKey();
    }

    // =========================================================================
    // SECTION 2: WEAK / INVALID ALGORITHMS
    // =========================================================================

    /**
     * 9. Uses broken HmacMD5 algorithm - VULNERABILITY
     */
    public byte[] weakAlgorithmHmacMd5(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacMD5"); // CC: ALGORITHM/InvalidValue "HmacMD5"
        mac.init(key);
        return mac.doFinal(data);
    }

    /**
     * 10. Uses weak HmacSHA1 algorithm - VULNERABILITY
     */
    public byte[] weakAlgorithmHmacSha1(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA1"); // CC: ALGORITHM/InvalidValue "HmacSHA1"
        mac.init(key);
        return mac.doFinal(data);
    }

    /**
     * 11. Uses weak HmacSHA224 algorithm - VULNERABILITY
     */
    public byte[] weakAlgorithmHmacSha224(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA224"); // CC: ALGORITHM/InvalidValue "HmacSHA224"
        mac.init(key);
        return mac.doFinal(data);
    }

    /**
     * 12. Uses weak PBEWithHmacSHA1 algorithm - VULNERABILITY
     */
    public byte[] weakAlgorithmPbeWithHmacSha1(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("PBEWithHmacSHA1"); // CC: ALGORITHM/InvalidValue "PBEWithHmacSHA1"
        mac.init(key);
        return mac.doFinal(data);
    }

    /**
     * 13. Weak algorithm selected via conditional branch - VULNERABILITY
     */
    public byte[] conditionalWeakAlgorithm(byte[] data, boolean legacyMode) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        String alg = legacyMode ? "HmacSHA1" : "HmacSHA256";
        Mac mac = Mac.getInstance(alg); // CC: ALGORITHM/InvalidValue "HmacSHA1"
        mac.init(key);
        return mac.doFinal(data);
    }

    // =========================================================================
    // SECTION 3: IMPROPER KEY MATERIAL
    // =========================================================================

    /**
     * 14. Hardcoded String key passed to SecretKeySpec and Mac - VULNERABILITY
     */
    public byte[] hardcodedStringKey(byte[] data) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec(HARDCODED_MAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"); // CC: KEY_MATERIAL/ForbiddenType "java.lang.String", KEY_MATERIAL/ImproperGenerated
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec); // CC: KEY_MATERIAL/ImproperGenerated
        return mac.doFinal(data);
    }

    /**
     * 15. Hardcoded static byte[] key passed to SecretKeySpec and Mac - VULNERABILITY
     */
    public byte[] hardcodedByteArrayKey(byte[] data) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec(STATIC_KEY_BYTES, "HmacSHA256"); // CC: KEY_MATERIAL/ImproperGenerated
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec); // CC: KEY_MATERIAL/ImproperGenerated
        return mac.doFinal(data);
    }

    /**
     * 16. Weak java.util.Random key passed to SecretKeySpec and Mac - VULNERABILITY
     */
    public byte[] weakUtilRandomKey(byte[] data) throws GeneralSecurityException {
        byte[] keyBytes = new byte[32];
        new Random().nextBytes(keyBytes);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256"); // CC: KEY_MATERIAL/ImproperGenerated
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec); // CC: KEY_MATERIAL/ImproperGenerated
        return mac.doFinal(data);
    }

    /**
     * 17. Key generated with predictable SecureRandom seed - VULNERABILITY
     */
    public byte[] keyFromPredictablySeededSecureRandom(byte[] data) throws GeneralSecurityException {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(987654321L); // CC: KEY_MATERIAL/ImproperGenerated

        byte[] keyBytes = new byte[32];
        sr.nextBytes(keyBytes);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256"); // CC: KEY_MATERIAL/ImproperGenerated
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec); // CC: KEY_MATERIAL/ImproperGenerated
        return mac.doFinal(data);
    }

    /**
     * 18. Combined weak algorithm AND hardcoded String key - VULNERABILITY
     */
    public byte[] weakAlgorithmAndHardcodedKey(byte[] data) throws GeneralSecurityException {
        SecretKeySpec keySpec = new SecretKeySpec("weakKey".getBytes(), "HmacMD5"); // CC: ALGORITHM/InvalidValue "HmacMD5", KEY_MATERIAL/ForbiddenType "java.lang.String", KEY_MATERIAL/ImproperGenerated
        Mac mac = Mac.getInstance("HmacMD5"); // CC: ALGORITHM/InvalidValue "HmacMD5"
        mac.init(keySpec); // CC: KEY_MATERIAL/ImproperGenerated
        return mac.doFinal(data);
    }

    // =========================================================================
    // SECTION 4: API MISUSE / SEQUENCE & TYPESTATE VIOLATIONS
    // =========================================================================

    /**
     * 19. Calling update() on an uninitialized Mac - VULNERABILITY
     */
    public byte[] uninitializedMacUpdate(byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.update(data); // CC: API_MISUSE/UnexpectedCall "Mac.update"
        return mac.doFinal();
    }

    /**
     * 20. Calling doFinal(byte[]) on an uninitialized Mac - VULNERABILITY
     */
    public byte[] uninitializedMacDoFinal(byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        return mac.doFinal(data); // CC: API_MISUSE/UnexpectedCall "Mac.doFinal"
    }

    /**
     * 21. Calling zero-arg doFinal() directly after init() without update() - VULNERABILITY
     */
    public byte[] doFinalWithoutUpdate() throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(); // CC: API_MISUSE/UnexpectedCall "Mac.doFinal"
    }

    /**
     * 22. Incomplete operation: getInstance() only, never initialized or finalized - VULNERABILITY
     */
    public Mac incompleteAfterGetInstance() throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256"); // CC: API_MISUSE/IncompleteOperation "javax.crypto.Mac"
        return mac;
    }

    /**
     * 23. Incomplete operation: init() called, never updated or finalized - VULNERABILITY
     */
    public void incompleteAfterInit() throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key); // CC: API_MISUSE/IncompleteOperation "javax.crypto.Mac"
    }

    /**
     * 24. Incomplete operation: update() called, doFinal() forgotten - VULNERABILITY
     */
    public void incompleteAfterUpdate(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        mac.update(data); // CC: API_MISUSE/IncompleteOperation "javax.crypto.Mac"
    }

    /**
     * 25. Double init() without an intervening doFinal() - VULNERABILITY
     */
    public byte[] doubleInitWithoutDoFinal(byte[] data) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key1 = keyGen.generateKey();
        SecretKey key2 = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key1);
        mac.init(key2); // CC: API_MISUSE/UnexpectedCall "Mac.init"
        return mac.doFinal(data);
    }

    // =========================================================================
    // SECTION 5: COMPLEX CONTROL-FLOW, LOOP, FIELD & INTERPROCEDURAL EDGE CASES
    // =========================================================================

    /**
     * 26. Loop-based update that may execute zero times before doFinal() - VULNERABILITY
     */
    public byte[] loopUpdatePotentiallySkipped(byte[][] chunks) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        for (byte[] chunk : chunks) {
            mac.update(chunk);
        }
        return mac.doFinal(); // CC: API_MISUSE/UnexpectedCall "Mac.doFinal"
    }

    /**
     * 27. Conditional early return leaves Mac incomplete on one branch - VULNERABILITY
     */
    public byte[] conditionalEarlyReturnIncomplete(byte[] data, boolean abort) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        mac.update(data); // CC: API_MISUSE/IncompleteOperation "javax.crypto.Mac"
        if (abort) {
            return new byte[0];
        }
        return mac.doFinal();
    }

    /**
     * 28. Conditional key selection: one branch valid, one branch hardcoded - VULNERABILITY
     */
    public byte[] conditionalMixedKeySources(byte[] data, boolean useFallback) throws GeneralSecurityException {
        SecretKey key;
        if (useFallback) {
            key = new SecretKeySpec(STATIC_KEY_BYTES, "HmacSHA256"); // CC: KEY_MATERIAL/ImproperGenerated
        } else {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            key = keyGen.generateKey();
        }

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key); // CC: KEY_MATERIAL/ImproperGenerated
        return mac.doFinal(data);
    }

    /**
     * 29. Interprocedural lifecycle: Mac initialized in helper, finalized in caller - NO ISSUE
     */
    public byte[] interproceduralValidLifecycle(byte[] data) throws GeneralSecurityException {
        Mac mac = createInitializedMac();
        mac.update(data);
        return mac.doFinal();
    }

    private Mac createInitializedMac() throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        SecretKey key = keyGen.generateKey();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac;
    }

    /**
     * 30. Field-stored keys: valid field vs. hardcoded field - VULNERABILITY on hardcoded field
     */
    public byte[] fieldStoredKeysTest(byte[] data, boolean useHardcoded) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        this.fieldValidKey = keyGen.generateKey();
        this.fieldHardcodedKey = new SecretKeySpec(HARDCODED_MAC_KEY.getBytes(), "HmacSHA256"); // CC: KEY_MATERIAL/ForbiddenType "java.lang.String", KEY_MATERIAL/ImproperGenerated

        Mac mac = Mac.getInstance("HmacSHA256");
        if (useHardcoded) {
            mac.init(this.fieldHardcodedKey); // CC: KEY_MATERIAL/ImproperGenerated
        } else {
            mac.init(this.fieldValidKey);
        }
        return mac.doFinal(data);
    }
}