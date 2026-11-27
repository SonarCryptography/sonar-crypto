package com.example.crypto;


import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Test cases for java.security.SecureRandom covering valid usages,
 * predictable/static seeding, invalid algorithms, downstream JCA consumers,
 * and complex control-flow/interprocedural edge cases.
 */
public class SecureRandomTests {

    private static final byte[] STATIC_SEED = new byte[] {
            10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 1, 2, 3, 4
    };
    private static final String HARDCODED_SEED_STRING = "NotARandomSeedAtAll";
    private static final long FIXED_LONG_SEED = 0xDEADBEEFL;

    private SecureRandom fieldSecureRandom;

    // =========================================================================
    // SECTION 1: VALID / NORMAL CASES (NO ISSUE)
    // =========================================================================

    /**
     * 1. Default SecureRandom constructor with nextBytes() - NO ISSUE
     */
    public byte[] validDefaultSecureRandom() {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[32];
        sr.nextBytes(bytes);
        return bytes;
    }

    /**
     * 2. SecureRandom.getInstanceStrong() - NO ISSUE
     */
    public byte[] validGetInstanceStrong() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] bytes = new byte[16];
        sr.nextBytes(bytes);
        return bytes;
    }

    /**
     * 3. SecureRandom.getInstance("NativePRNG") - NO ISSUE
     */
    public byte[] validNativePrng() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("NativePRNG");
        byte[] bytes = new byte[16];
        sr.nextBytes(bytes);
        return bytes;
    }

    /**
     * 4. SecureRandom.getInstance("DRBG") - NO ISSUE
     */
    public byte[] validDrbg() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("DRBG");
        byte[] bytes = new byte[32];
        sr.nextBytes(bytes);
        return bytes;
    }

    /**
     * 5. SecureRandom.getInstance("SHA1PRNG") - NO ISSUE (allowed in CrySL JCA rules)
     */
    public byte[] validSha1Prng() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] bytes = new byte[16];
        sr.nextBytes(bytes);
        return bytes;
    }

    /**
     * 6. Reseeding SecureRandom with its own generateSeed() - NO ISSUE
     */
    public byte[] validReseedWithGenerateSeed() {
        SecureRandom sr = new SecureRandom();
        byte[] seed = sr.generateSeed(32);
        sr.setSeed(seed);

        byte[] output = new byte[32];
        sr.nextBytes(output);
        return output;
    }

    /**
     * 7. Reseeding SecureRandom with bytes from another SecureRandom - NO ISSUE
     */
    public byte[] validReseedFromAnotherSecureRandom() {
        SecureRandom entropySource = new SecureRandom();
        byte[] seed = new byte[32];
        entropySource.nextBytes(seed);

        SecureRandom sr = new SecureRandom(seed);
        byte[] output = new byte[16];
        sr.nextBytes(output);
        return output;
    }

    /**
     * 8. Valid SecureRandom used to generate GCM IV for AES/GCM/NoPadding - NO ISSUE
     */
    public byte[] validSecureRandomForGcmIv(byte[] plaintext) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();

        SecureRandom sr = new SecureRandom();
        byte[] iv = new byte[12];
        sr.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(plaintext);
    }

    /**
     * 9. Valid SecureRandom passed to KeyGenerator.init(256, sr) - NO ISSUE
     */
    public SecretKey validSecureRandomForKeyGenerator() throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, sr);
        return keyGen.generateKey();
    }

    /**
     * 10. Valid SecureRandom passed to KeyPairGenerator.initialize(2048, sr) - NO ISSUE
     */
    public KeyPair validSecureRandomForKeyPairGenerator() throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048, sr);
        return kpg.generateKeyPair();
    }

    /**
     * 11. Valid SecureRandom used to generate PBE salt - NO ISSUE
     */
    public PBEParameterSpec validSecureRandomForPbeSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return new PBEParameterSpec(salt, 65536);
    }

    // =========================================================================
    // SECTION 2: PREDICTABLE / STATIC SEEDING VULNERABILITIES
    // =========================================================================

    /**
     * 12. Constant long passed to setSeed(long) - VULNERABILITY
     */
    public byte[] predictableConstantLongSeed() {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(123456789L); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 13. Zero seed passed to setSeed(0L) - VULNERABILITY
     */
    public byte[] predictableZeroLongSeed() {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(0L); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 14. Static final long constant passed to setSeed(long) - VULNERABILITY
     */
    public byte[] predictableStaticFieldLongSeed() {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(FIXED_LONG_SEED); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 15. System.currentTimeMillis() used as seed (predictable timestamp) - VULNERABILITY
     */
    public byte[] predictableTimestampSeed() {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(System.currentTimeMillis()); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 16. System.nanoTime() used as seed - VULNERABILITY
     */
    public byte[] predictableNanoTimeSeed() {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(System.nanoTime()); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 17. Hardcoded byte[] passed to setSeed(byte[]) - VULNERABILITY
     */
    public byte[] predictableStaticByteArraySeed() {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(STATIC_SEED); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 18. Hardcoded String.getBytes() passed to setSeed(byte[]) - VULNERABILITY
     */
    public byte[] predictableStringBytesSeed() {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(HARDCODED_SEED_STRING.getBytes(StandardCharsets.UTF_8)); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 19. Hardcoded byte[] passed to SecureRandom(byte[]) constructor - VULNERABILITY
     */
    public byte[] predictableConstructorByteArraySeed() {
        SecureRandom sr = new SecureRandom(STATIC_SEED); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 20. Hardcoded String.getBytes() passed to SecureRandom(byte[]) constructor - VULNERABILITY
     */
    public byte[] predictableConstructorStringSeed() {
        SecureRandom sr = new SecureRandom("fixed-seed".getBytes()); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 21. Seeding SecureRandom with java.util.Random output - VULNERABILITY
     */
    public byte[] seededFromInsecureUtilRandom() {
        byte[] weakSeed = new byte[16];
        new Random().nextBytes(weakSeed);

        SecureRandom sr = new SecureRandom();
        sr.setSeed(weakSeed); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    // =========================================================================
    // SECTION 3: INVALID ALGORITHM NAMES
    // =========================================================================

    /**
     * 22. Invalid / non-standard PRNG algorithm name - VULNERABILITY
     */
    public byte[] invalidAlgorithmName() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("MD5PRNG"); // CC: ALGORITHM/InvalidValue "MD5PRNG"
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    // =========================================================================
    // SECTION 4: COMPLEX DATAFLOW, PROPAGATION & CONTROL-FLOW EDGE CASES
    // =========================================================================

    /**
     * 23. Predictable SecureRandom propagating taint into AES SecretKeySpec and Cipher - VULNERABILITY
     */
    public byte[] taintedSecureRandomToSecretKeyAndCipher(byte[] plaintext) throws GeneralSecurityException {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(42L); // CC: KEY_MATERIAL/ImproperGenerated

        byte[] keyBytes = new byte[16];
        sr.nextBytes(keyBytes);
        byte[] iv = new byte[12];
        sr.nextBytes(iv);

        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES"); // CC: KEY_MATERIAL/ImproperGenerated
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // CC: KEY_MATERIAL/ImproperGenerated

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec); // CC: KEY_MATERIAL/ImproperGenerated
        return cipher.doFinal(plaintext);
    }

    /**
     * 24. Conditional predictable seeding on one branch only - VULNERABILITY
     */
    public byte[] conditionalPredictableSeed(boolean deterministicTestMode) {
        SecureRandom sr = new SecureRandom();
        if (deterministicTestMode) {
            sr.setSeed(1337L); // CC: KEY_MATERIAL/ImproperGenerated
        }
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 25. Interprocedural predictable seeding inside a helper method - VULNERABILITY
     */
    public byte[] interproceduralPredictableSeed() {
        SecureRandom sr = createBrokenSecureRandom();
        byte[] iv = new byte[16];
        sr.nextBytes(iv);
        return new IvParameterSpec(iv).getIV(); // CC: KEY_MATERIAL/ImproperGenerated
    }

    private SecureRandom createBrokenSecureRandom() {
        SecureRandom sr = new SecureRandom();
        sr.setSeed(new byte[] { 1, 2, 3, 4 }); // CC: KEY_MATERIAL/ImproperGenerated
        return sr;
    }

    /**
     * 26. Reseeding with a constant AFTER an initial nextBytes() call - VULNERABILITY
     */
    public byte[] reseedWithConstantAfterInitialUse() {
        SecureRandom sr = new SecureRandom();
        byte[] first = new byte[16];
        sr.nextBytes(first);

        sr.setSeed(999L); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] second = new byte[16];
        sr.nextBytes(second);
        return second;
    }

    /**
     * 27. Loop repeatedly reseeding SecureRandom with loop counter - VULNERABILITY
     */
    public byte[] loopPredictableReseed(int rounds) {
        SecureRandom sr = new SecureRandom();
        byte[] out = new byte[16];
        for (int i = 0; i < rounds; i++) {
            sr.setSeed((long) i); // CC: KEY_MATERIAL/ImproperGenerated
            sr.nextBytes(out);
        }
        return out;
    }

    /**
     * 28. Field-stored SecureRandom poisoned by static seed and consumed downstream - VULNERABILITY
     */
    public GCMParameterSpec fieldStoredPoisonedSecureRandom() {
        this.fieldSecureRandom = new SecureRandom();
        this.fieldSecureRandom.setSeed(STATIC_SEED); // CC: KEY_MATERIAL/ImproperGenerated

        byte[] iv = new byte[12];
        this.fieldSecureRandom.nextBytes(iv);
        return new GCMParameterSpec(128, iv); // CC: KEY_MATERIAL/ImproperGenerated
    }
}