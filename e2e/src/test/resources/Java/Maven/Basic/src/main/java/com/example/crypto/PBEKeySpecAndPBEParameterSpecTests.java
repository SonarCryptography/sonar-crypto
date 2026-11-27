package com.example.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Static-analysis fixtures for PBEKeySpec and PBEParameterSpec.
 *
 * 24 PBEKeySpec cases, 15 PBEParameterSpec cases, and one combined PBE case.
 * These are source fixtures, not a runnable JUnit suite.
 *
 * Expectations target the repository's pinned JCA CrySL rules.
 * "NO ISSUE" means no finding expected under those rules, not that the
 * complete operation is necessarily suitable for production.
 *
 * Password arguments represent externally supplied, non-hardcoded,
 * non-null char[] values that do not originate from String.
 * Inputs remain caller-owned except in tests explicitly exercising wiping.
 *
 * PBEKeySpec clones its password and salt. clearPassword() clears only
 * the internal password copy. getPassword() returns another independent
 * copy, which must be wiped separately when no longer needed.
 *
 * The 10,000-iteration cases test the pinned rule boundary; they are not
 * current production work-factor recommendations.
 *
 * JCA/CrySL distinction:
 * - PBEKeySpec requires non-empty, non-null salt and positive iterations;
 *   its four-argument constructor also requires positive key length.
 * - OpenJDK PBEParameterSpec rejects null salt but does not validate salt
 *   length or iteration-count positivity at construction.
 * - The pinned rules impose no minimum salt length or PBEKeySpec key length.
 *
 * DO NOT use the intentionally insecure or runtime-error cases in production.
 */
public class PBEKeySpecAndPBEParameterSpecTests {

    private static final int ITERATIONS = 600_000;

    // =========================================================================
    // PBEKeySpec: VALID USAGES
    // =========================================================================

    /** 01. PBKDF2-HMAC-SHA256 with fresh salt and finally cleanup - NO ISSUE. */
    public SecretKey validPbkdf2WithFinallyCleanup(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 02. Exact iteration-count boundary - NO ISSUE.
     * A specification need not be passed to a factory to complete its lifecycle.
     */
    public int validKeySpecAtIterationBoundary(char[] password) {
        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);
        try {
            return spec.getIterationCount();
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 03. Interface alias and cleanup performed in a helper - NO ISSUE.
     * The alias and original reference identify the same specification.
     */
    public int validKeySpecAliasAndHelperCleanup(char[] password) {
        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);
        KeySpec alias = spec;

        try {
            return ((PBEKeySpec) alias).getKeyLength();
        } finally {
            clearKeySpec((PBEKeySpec) alias);
        }
    }

    /**
     * 04. Specification constructed in a helper, used and cleared by caller -
     * NO ISSUE. Exercises interprocedural predicate and lifecycle propagation.
     */
    public SecretKey validKeySpecCreatedByHelper(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec = createValidKeySpec(password);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 05. Wiping the input does not wipe the specification's password clone -
     * NO ISSUE. This test takes ownership of and wipes the input array.
     */
    public boolean validPasswordDefensiveCopy(char[] password) {
        char[] expected = password.clone();
        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);

        char[] exported = null;
        try {
            Arrays.fill(password, '\0');
            exported = spec.getPassword();
            return Arrays.equals(expected, exported);
        } finally {
            if (exported != null) {
                Arrays.fill(exported, '\0');
            }
            Arrays.fill(expected, '\0');
            spec.clearPassword();
        }
    }

    /**
     * 06. Both the getPassword() copy and internal copy are wiped - NO ISSUE.
     * Clearing the specification alone would not clear the exported array.
     */
    public int validExportedPasswordCopyCleanup(char[] password) {
        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);

        try {
            char[] exported = spec.getPassword();
            try {
                return exported.length;
            } finally {
                Arrays.fill(exported, '\0');
            }
        } finally {
            spec.clearPassword();
        }
    }

    // =========================================================================
    // PBEKeySpec: CONSTRAINT, PASSWORD AND SALT FINDINGS
    // =========================================================================

    /** 07. One iteration below the pinned minimum. Legal JCA, invalid CrySL. */
    public void keySpecBelowIterationMinimum(char[] password) {
        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), ITERATIONS - 1, 256); // CC: GENERAL/Undefined
        spec.clearPassword();
    }

    /** 08. Low iteration count returned by a helper; finding at the consumer. */
    public void keySpecLowIterationsFromHelper(char[] password) {
        int iterations = belowMinimumIterations();

        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), iterations, 256); // CC: GENERAL/Undefined
        spec.clearPassword();
    }

    /**
     * 09. External String converted to char[] violates neverTypeOf.
     * The input String is not a literal, isolating the forbidden-type case.
     */
    public void keySpecPasswordOriginatesFromString(String password) {
        char[] characters = password.toCharArray();

        PBEKeySpec spec = new PBEKeySpec(characters, freshSalt(), ITERATIONS, 256); // CC: KEY_MATERIAL/ForbiddenType "java.lang.String"
        try {
            // Construction is the operation under test.
        } finally {
            spec.clearPassword();
            Arrays.fill(characters, '\0');
        }
    }

    /**
     * 10. Literal char[] violates notHardCoded without involving String.
     * Wiping it afterward does not change its hardcoded origin.
     */
    public void keySpecHardcodedCharArrayPassword() {
        char[] password = new char[] {
                'p', 'a', 's', 's', 'w', 'o', 'r', 'd'
        };

        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), ITERATIONS, 256); // CC: GENERAL/Undefined
        try {
            // Construction is the operation under test.
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }

    /**
     * 11. Fixed salt lacks randomized[salt].
     * The invalid specification also fails the factory's speccedKey requirement.
     */
    public SecretKey keySpecFixedSaltPropagatesToFactory(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] salt = new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16
        };

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256); // CC: KEY_MATERIAL/ImproperGenerated
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
        }
    }

    /** 12. java.util.Random does not establish randomized[salt]. */
    public void keySpecSaltFromWeakRandom(char[] password) {
        byte[] salt = new byte[16];
        new Random().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256); // CC: KEY_MATERIAL/ImproperGenerated
        spec.clearPassword();
    }

    /**
     * 13. Salt has secure and insecure sources on different branches.
     * The annotated finding is expected for the insecure-source path.
     */
    public void keySpecMixedSaltSources(
            char[] password, boolean secureSource) {
        byte[] salt = new byte[16];

        if (secureSource) {
            new SecureRandom().nextBytes(salt);
        } else {
            new Random().nextBytes(salt);
        }

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256); // CC: KEY_MATERIAL/ImproperGenerated
        spec.clearPassword();
    }

    /**
     * 14. Password-only constructor is explicitly forbidden by the rule.
     * This overload is legal JCA; the diagnostic is rule-policy-specific.
     */
    public void forbiddenPasswordOnlyKeySpec(char[] password) {
        PBEKeySpec spec = new PBEKeySpec(password); // CC: FORBIDDEN_METHOD/ForbiddenMethod
        spec.clearPassword();
    }

    /**
     * 15. Three-argument constructor is explicitly forbidden by the rule.
     * JCA supports it for fixed-key-size PBE mechanisms; PBKDF2 normally
     * requires an explicitly specified derived-key length.
     */
    public void forbiddenKeySpecWithoutKeyLength(char[] password) {
        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), ITERATIONS); // CC: FORBIDDEN_METHOD/ForbiddenMethod
        spec.clearPassword();
    }

    // =========================================================================
    // PBEKeySpec: LIFECYCLE FINDINGS
    // =========================================================================

    /** 16. Constructed specification is abandoned without clearPassword(). */
    public void keySpecNeverCleared(char[] password) {
        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), ITERATIONS, 256); // CC: API_MISUSE/IncompleteOperation "javax.crypto.spec.PBEKeySpec"
    }

    /**
     * 17. Wiping the caller's array does not clear the internal password copy.
     * This test takes ownership of and wipes the input array.
     */
    public void keySpecOnlyCallerPasswordCleared(char[] password) {
        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), ITERATIONS, 256); // CC: API_MISUSE/IncompleteOperation "javax.crypto.spec.PBEKeySpec"
        Arrays.fill(password, '\0');
    }

    /**
     * 18. Early return skips cleanup on one feasible path.
     * Unlike a finally block, the trailing clear does not cover that path.
     */
    public void keySpecEarlyReturnSkipsCleanup(
            char[] password, boolean abort) {
        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), ITERATIONS, 256); // CC: API_MISUSE/IncompleteOperation "javax.crypto.spec.PBEKeySpec"

        if (abort) {
            return;
        }

        spec.clearPassword();
    }

    /**
     * 19. Repeated clearPassword(): legal and harmless in OpenJDK, but the
     * pinned rule permits exactly one ClearPass event.
     */
    public void keySpecClearedTwice(char[] password) {
        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);

        spec.clearPassword();
        spec.clearPassword(); // CC: API_MISUSE/UnexpectedCall "PBEKeySpec.clearPassword"
    }

    /**
     * 20. Clearing negates speccedKey before generation.
     * A normal PBKDF2 provider also cannot read the cleared password.
     */
    public SecretKey keySpecClearedBeforeGeneration(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);
        spec.clearPassword();

        return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
    }

    // =========================================================================
    // PBEKeySpec: API / RULE-COVERAGE DISTINCTIONS
    // =========================================================================

    /**
     * 21. NO CRYSL FINDING EXPECTED, but getPassword() throws
     * IllegalStateException at runtime after clearing.
     * The pinned rule does not model getter calls.
     */
    public char[] keySpecGetPasswordAfterClear(char[] password) {
        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);

        spec.clearPassword();
        return spec.getPassword(); // API ONLY: throws IllegalStateException.
    }

    /**
     * 22. NO CRYSL FINDING EXPECTED for the exported password copy.
     * The internal lifecycle is complete, but the returned array still
     * contains password material and must be wiped by its recipient.
     */
    public char[] keySpecExportsPasswordCopy(char[] password) {
        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);

        try {
            return spec.getPassword();
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 23. One-byte securely generated salt - NO CRYSL FINDING EXPECTED.
     * The constructor accepts it and the rule has no salt-length minimum.
     * This is intentionally inadequate for a production salt.
     */
    public void keySpecVeryShortRandomSalt(char[] password) {
        byte[] salt = new byte[1];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        spec.clearPassword();
    }

    /**
     * 24. 64-bit requested key length - NO CRYSL FINDING EXPECTED.
     * It is positive, hence constructor-valid, but the rule imposes no
     * minimum derived-key strength.
     */
    public int keySpecShortRequestedKeyLength(char[] password) {
        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), ITERATIONS, 64);

        try {
            return spec.getKeyLength();
        } finally {
            spec.clearPassword();
        }
    }

    // =========================================================================
    // PBEParameterSpec: VALID USAGES
    // =========================================================================

    /**
     * 25. Two-argument constructor with fresh salt - NO ISSUE.
     * Construction alone completes this rule; no cleanup operation exists.
     */
    public PBEParameterSpec validTwoArgumentParameters() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        return new PBEParameterSpec(salt, ITERATIONS);
    }

    /**
     * 26. Three-argument constructor with a securely generated IV - NO ISSUE.
     * The IvParameterSpec is independently checked by its own CrySL rule.
     */
    public PBEParameterSpec validParametersWithIv() {
        byte[] salt = freshSalt();

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        return new PBEParameterSpec(salt, ITERATIONS, ivSpec);
    }

    /**
     * 27. Explicit null nested specification - NO ISSUE.
     * Null is permitted by JCA and is not forbidden by the PBEParameterSpec rule.
     */
    public PBEParameterSpec validParametersWithNullNestedSpec() {
        return new PBEParameterSpec(freshSalt(), ITERATIONS, null);
    }

    /** 28. Exact pinned iteration-count boundary - NO ISSUE. */
    public PBEParameterSpec validParametersAtIterationBoundary() {
        return new PBEParameterSpec(freshSalt(), 10_000);
    }

    /**
     * 29. Interface alias and repeated metadata access - NO ISSUE.
     * Getters do not add lifecycle obligations.
     */
    public boolean validParameterInterfaceAlias() {
        PBEParameterSpec parameters =
                new PBEParameterSpec(freshSalt(), ITERATIONS);
        AlgorithmParameterSpec alias = parameters;
        PBEParameterSpec recovered = (PBEParameterSpec) alias;

        return recovered.getIterationCount() == ITERATIONS
                && recovered.getSalt().length == 16
                && recovered.getParameterSpec() == null;
    }

    /**
     * 30. Constructor and getSalt() both make defensive salt copies -
     * NO ISSUE. Mutating either caller-owned array leaves stored salt unchanged.
     */
    public boolean validParameterSaltDefensiveCopies() {
        byte[] salt = freshSalt();
        byte[] expected = salt.clone();

        PBEParameterSpec parameters =
                new PBEParameterSpec(salt, ITERATIONS);

        Arrays.fill(salt, (byte) 0);

        byte[] exported = parameters.getSalt();
        boolean constructorCopiedSalt = Arrays.equals(expected, exported);

        Arrays.fill(exported, (byte) 0);

        return constructorCopiedSalt
                && Arrays.equals(expected, parameters.getSalt());
    }

    // =========================================================================
    // PBEParameterSpec: SALT AND ITERATION FINDINGS
    // =========================================================================

    /** 31. Fixed salt does not satisfy randomized[salt]. */
    public PBEParameterSpec parametersWithFixedSalt() {
        byte[] salt = new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16
        };

        return new PBEParameterSpec(salt, ITERATIONS); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /** 32. Salt generated with java.util.Random is not rule-approved. */
    public PBEParameterSpec parametersWithWeakRandomSalt() {
        byte[] salt = new byte[16];
        new Random().nextBytes(salt);

        return new PBEParameterSpec(salt, ITERATIONS); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 33. Randomization happens too late.
     * The constructor has already copied the original all-zero salt.
     */
    public PBEParameterSpec parametersRandomizedAfterConstruction() {
        byte[] salt = new byte[16];

        PBEParameterSpec parameters = new PBEParameterSpec(salt, ITERATIONS); // CC: KEY_MATERIAL/ImproperGenerated

        new SecureRandom().nextBytes(salt);
        return parameters;
    }

    /** 34. One iteration below the pinned minimum. */
    public PBEParameterSpec parametersBelowIterationMinimum() {
        return new PBEParameterSpec(freshSalt(), 9_999); // CC: GENERAL/Undefined
    }

    /**
     * 35. Zero iterations: OpenJDK accepts construction, but CrySL rejects it.
     * A downstream PBE implementation may reject these parameters at use.
     */
    public PBEParameterSpec parametersWithZeroIterations() {
        return new PBEParameterSpec(freshSalt(), 0); // CC: GENERAL/Undefined
    }

    /**
     * 36. Negative iterations: also accepted by this OpenJDK constructor,
     * but rejected by the pinned iteration-count constraint.
     */
    public PBEParameterSpec parametersWithNegativeIterations() {
        return new PBEParameterSpec(freshSalt(), -1); // CC: GENERAL/Undefined
    }

    /**
     * 37. Secure PBE salt with a predictable nested IV.
     * The finding belongs to IvParameterSpec, not PBEParameterSpec:
     * the outer rule does not require preparedIV for its nested argument.
     */
    public PBEParameterSpec parametersWithPredictableNestedIv() {
        byte[] salt = freshSalt();
        byte[] iv = new byte[16];

        IvParameterSpec ivSpec = new IvParameterSpec(iv); // CC: KEY_MATERIAL/ImproperGenerated

        return new PBEParameterSpec(salt, ITERATIONS, ivSpec);
    }

    // =========================================================================
    // PBEParameterSpec: RULE-COVERAGE DISTINCTIONS
    // =========================================================================

    /**
     * 38. One-byte securely generated salt - NO CRYSL FINDING EXPECTED.
     * Neither the constructor nor the pinned rule imposes a minimum length.
     * An actual PBE provider can impose additional algorithm-specific limits.
     */
    public PBEParameterSpec parametersWithVeryShortRandomSalt() {
        byte[] salt = new byte[1];
        new SecureRandom().nextBytes(salt);

        return new PBEParameterSpec(salt, ITERATIONS);
    }

    /**
     * 39. Reuse of one randomized salt - NO CRYSL FINDING EXPECTED.
     * randomized expresses approved origin, not per-operation freshness.
     * Reusing salt across independent encryption records is not recommended.
     */
    public PBEParameterSpec[] parametersReusingRandomizedSalt() {
        byte[] salt = freshSalt();

        PBEParameterSpec first = new PBEParameterSpec(salt, ITERATIONS);
        PBEParameterSpec second = new PBEParameterSpec(salt, ITERATIONS);

        return new PBEParameterSpec[] {first, second};
    }

    // =========================================================================
    // COMBINED PBEKeySpec + PBEParameterSpec + FACTORY + CIPHER
    // =========================================================================

    /**
     * 40. Rule-conforming password-based encryption - NO ISSUE EXPECTED.
     *
     * PBEKeySpec supplies key-construction input.
     * PBEParameterSpec supplies PBE cipher parameters, including an AES IV.
     * The same salt is intentionally used within this single PBE operation.
     *
     * This exercises the PBE mechanism, not PBKDF2 followed by raw AES.
     * It is not a complete authenticated-encryption or ciphertext-storage design.
     */
    public byte[] validCombinedPbeEncryption(
            char[] password, byte[] plaintext)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");

        byte[] salt = freshSalt();
        PBEKeySpec keySpec =
                new PBEKeySpec(password, salt, ITERATIONS, 128);

        SecretKey key;
        try {
            key = factory.generateSecret(keySpec);
        } finally {
            keySpec.clearPassword();
        }

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        PBEParameterSpec parameters =
                new PBEParameterSpec(salt, ITERATIONS, ivSpec);

        Cipher cipher =
                Cipher.getInstance(
                        "PBEWithHmacSHA256AndAES_128/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, parameters);
        return cipher.doFinal(plaintext);
    }

    // =========================================================================
    // HELPERS: not additional numbered fixtures
    // =========================================================================

    private static byte[] freshSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static PBEKeySpec createValidKeySpec(char[] password) {
        return new PBEKeySpec(password, freshSalt(), ITERATIONS, 256);
    }

    private static void clearKeySpec(PBEKeySpec spec) {
        spec.clearPassword();
    }

    private static int belowMinimumIterations() {
        return 9_999;
    }
}