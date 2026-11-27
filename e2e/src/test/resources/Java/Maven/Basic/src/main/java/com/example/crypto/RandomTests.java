package com.example.crypto;



import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Test cases for java.util.Random (and related non-cryptographic RNGs) across
 * non-cryptographic contexts (NO ISSUE in CogniCrypt) and cryptographic contexts
 * where weak randomness flows into JCA APIs (VULNERABILITY).
 * DO NOT use this code in production!
 */
public class RandomTests {

    private final Random fieldWeakRandom = new Random();
    private final SecureRandom fieldStrongRandom = new SecureRandom();

    // =========================================================================
    // SECTION 1: NON-CRYPTOGRAPHIC CONTEXTS (NO ISSUE IN COGNICRYPT)
    // =========================================================================

    /**
     * 1. java.util.Random nextBytes() in non-cryptographic context - NO ISSUE
     */
    public byte[] nonCryptoRandomBytes() {
        Random random = new Random();
        byte[] buf = new byte[16];
        random.nextBytes(buf);
        return buf;
    }

    /**
     * 2. java.util.Random with fixed seed in non-cryptographic context (e.g., simulation) - NO ISSUE
     */
    public int nonCryptoSeededRandomInt() {
        Random random = new Random(42L);
        return random.nextInt(100);
    }

    /**
     * 3. ThreadLocalRandom in non-cryptographic context - NO ISSUE
     */
    public long nonCryptoThreadLocalRandom() {
        return ThreadLocalRandom.current().nextLong();
    }

    /**
     * 4. SplittableRandom in non-cryptographic context - NO ISSUE
     */
    public int nonCryptoSplittableRandom() {
        SplittableRandom sr = new SplittableRandom(1234L);
        return sr.nextInt(1000);
    }

    /**
     * 5. Math.random() in non-cryptographic context - NO ISSUE
     */
    public double nonCryptoMathRandom() {
        return Math.random();
    }

    /**
     * 6. Polymorphic Random reference backed by SecureRandom used for GCM IV - NO ISSUE
     */
    public byte[] polymorphicSecureRandomValid(byte[] plaintext) throws GeneralSecurityException {
        Random rng = new SecureRandom();
        byte[] iv = new byte[12];
        rng.nextBytes(iv);

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return cipher.doFinal(plaintext);
    }

    // =========================================================================
    // SECTION 2: DIRECT CRYPTOGRAPHIC MISUSE OF java.util.Random
    // =========================================================================

    /**
     * 7. java.util.Random bytes used as AES SecretKeySpec - VULNERABILITY
     */
    public SecretKey weakRandomForAesKey() {
        Random random = new Random();
        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, "AES"); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 8. java.util.Random bytes used as IvParameterSpec - VULNERABILITY
     */
    public IvParameterSpec weakRandomForIvParameterSpec() {
        Random random = new Random();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return new IvParameterSpec(iv); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 9. java.util.Random bytes used as GCMParameterSpec in AES/GCM - VULNERABILITY
     */
    public byte[] weakRandomForGcmEncryption(byte[] plaintext) throws GeneralSecurityException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();

        Random random = new Random();
        byte[] iv = new byte[12];
        random.nextBytes(iv);

        GCMParameterSpec spec = new GCMParameterSpec(128, iv); // CC: KEY_MATERIAL/ImproperGenerated
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, spec); // CC: KEY_MATERIAL/ImproperGenerated
        return cipher.doFinal(plaintext);
    }

    /**
     * 10. java.util.Random bytes used as salt in PBEParameterSpec - VULNERABILITY
     */
    public PBEParameterSpec weakRandomForPbeParameterSpec() {
        Random random = new Random();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return new PBEParameterSpec(salt, 65536); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 11. java.util.Random bytes used as salt in PBEKeySpec / SecretKeyFactory - VULNERABILITY
     */
    public SecretKey weakRandomForPbeKeySpec(char[] password) throws GeneralSecurityException {
        Random random = new Random();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, 65536, 256); // CC: KEY_MATERIAL/ImproperGenerated
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey key = skf.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        spec.clearPassword();
        return key;
    }

    /**
     * 12. java.util.Random long passed to SecureRandom.setSeed(long) - VULNERABILITY
     */
    public byte[] weakRandomLongSeedingSecureRandom() {
        Random random = new Random();
        SecureRandom sr = new SecureRandom();
        sr.setSeed(random.nextLong()); // CC: KEY_MATERIAL/ImproperGenerated

        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 13. java.util.Random byte[] passed to SecureRandom(byte[]) constructor - VULNERABILITY
     */
    public byte[] weakRandomBytesSeedingSecureRandomConstructor() {
        Random random = new Random();
        byte[] seed = new byte[16];
        random.nextBytes(seed);

        SecureRandom sr = new SecureRandom(seed); // CC: KEY_MATERIAL/ImproperGenerated
        byte[] out = new byte[16];
        sr.nextBytes(out);
        return out;
    }

    /**
     * 14. java.util.Random bytes used as HMAC key - VULNERABILITY
     */
    public byte[] weakRandomForHmacKey(byte[] data) throws GeneralSecurityException {
        Random random = new Random();
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256"); // CC: KEY_MATERIAL/ImproperGenerated
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(keySpec); // CC: KEY_MATERIAL/ImproperGenerated
        return mac.doFinal(data);
    }

    /**
     * 15. Seeded java.util.Random(0L) used for AES key - VULNERABILITY
     */
    public SecretKey seededWeakRandomForAesKey() {
        Random random = new Random(0L);
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, "AES"); // CC: KEY_MATERIAL/ImproperGenerated
    }

    // =========================================================================
    // SECTION 3: OTHER NON-CRYPTO RNGs IN CRYPTO CONTEXTS
    // =========================================================================

    /**
     * 16. ThreadLocalRandom used to generate GCM IV - VULNERABILITY
     */
    public GCMParameterSpec threadLocalRandomForGcmIv() {
        byte[] iv = new byte[12];
        ThreadLocalRandom.current().nextBytes(iv);
        return new GCMParameterSpec(128, iv); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 17. SplittableRandom used to fill AES key bytes - VULNERABILITY
     */
    public SecretKey splittableRandomForAesKey() {
        SplittableRandom rng = new SplittableRandom();
        byte[] keyBytes = new byte[16];
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = (byte) rng.nextInt(256);
        }
        return new SecretKeySpec(keyBytes, "AES"); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 18. Math.random() used to build an IV byte array - VULNERABILITY
     */
    public IvParameterSpec mathRandomForIv() {
        byte[] iv = new byte[16];
        for (int i = 0; i < iv.length; i++) {
            iv[i] = (byte) (Math.random() * 256);
        }
        return new IvParameterSpec(iv); // CC: KEY_MATERIAL/ImproperGenerated
    }

    // =========================================================================
    // SECTION 4: COMPLEX DATAFLOW, BRANCHING, ALIASING & INTERPROCEDURAL CASES
    // =========================================================================

    /**
     * 19. Interprocedural: helper returns java.util.Random bytes used as GCM IV - VULNERABILITY
     */
    public GCMParameterSpec interproceduralWeakRandomIv() {
        byte[] iv = generateWeakBytes(12);
        return new GCMParameterSpec(128, iv); // CC: KEY_MATERIAL/ImproperGenerated
    }

    private byte[] generateWeakBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }

    /**
     * 20. Conditional branch: SecureRandom on true branch, Random on false branch - VULNERABILITY
     */
    public GCMParameterSpec conditionalMixedRandomness(boolean secureMode) {
        byte[] iv = new byte[12];
        if (secureMode) {
            new SecureRandom().nextBytes(iv);
        } else {
            new Random().nextBytes(iv);
        }
        return new GCMParameterSpec(128, iv); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 21. Polymorphic Random variable assigned SecureRandom or Random depending on branch - VULNERABILITY
     */
    public IvParameterSpec polymorphicConditionalRng(boolean useStrong) {
        Random rng = useStrong ? new SecureRandom() : new Random();
        byte[] iv = new byte[16];
        rng.nextBytes(iv);
        return new IvParameterSpec(iv); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 22. Copying java.util.Random bytes via System.arraycopy into IV - VULNERABILITY
     */
    public IvParameterSpec arrayCopyFromWeakRandom() {
        byte[] source = new byte[16];
        new Random().nextBytes(source);

        byte[] iv = new byte[16];
        System.arraycopy(source, 0, iv, 0, source.length);
        return new IvParameterSpec(iv); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 23. Copying java.util.Random bytes via Arrays.copyOf into AES key - VULNERABILITY
     */
    public SecretKey arraysCopyOfFromWeakRandom() {
        byte[] raw = new byte[32];
        new Random().nextBytes(raw);

        byte[] keyBytes = Arrays.copyOf(raw, 16);
        return new SecretKeySpec(keyBytes, "AES"); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 24. Loop filling key bytes via Random.nextInt() instead of nextBytes() - VULNERABILITY
     */
    public SecretKey loopFilledWeakRandomKey() {
        Random random = new Random();
        byte[] keyBytes = new byte[16];
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = (byte) random.nextInt(256);
        }
        return new SecretKeySpec(keyBytes, "AES"); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 25. Field-level Random used to generate AES/GCM IV - VULNERABILITY
     */
    public GCMParameterSpec fieldWeakRandomForGcm() {
        byte[] iv = new byte[12];
        this.fieldWeakRandom.nextBytes(iv);
        return new GCMParameterSpec(128, iv); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 26. Field-level SecureRandom used to generate AES/GCM IV - NO ISSUE
     */
    public GCMParameterSpec fieldStrongRandomForGcm() {
        byte[] iv = new byte[12];
        this.fieldStrongRandom.nextBytes(iv);
        return new GCMParameterSpec(128, iv);
    }
}