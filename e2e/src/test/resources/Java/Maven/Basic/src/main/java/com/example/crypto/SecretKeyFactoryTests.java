package com.example.crypto;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Static-analysis fixtures for javax.crypto.SecretKeyFactory.
 *
 * Expectations target the repository's pinned JCA CrySL rules.
 * These are scanner fixtures, not JUnit tests or production examples.
 *
 * "NO ISSUE" means no finding expected under those rules. It does not mean
 * that every parameter choice is a current production recommendation.
 *
 * Unless a case explicitly tests another source, passwords are externally
 * supplied, non-hardcoded char[] values. They remain caller-owned:
 * PBEKeySpec.clearPassword() clears the specification's internal copy,
 * not the caller's original array.
 *
 * Provider arguments must be non-null and support the requested operation,
 * except where a case deliberately exercises an unsupported-provider path.
 *
 * The shared freshSalt() helper intentionally exercises interprocedural
 * propagation of the randomized predicate.
 */
public class SecretKeyFactoryTests {

    private String fieldAlgorithm;

    // =========================================================================
    // SECTION 1: VALID USAGES / FALSE-POSITIVE REGRESSION CASES
    // =========================================================================

    /** 01. PBKDF2-HMAC-SHA256 with fresh salt and guaranteed cleanup - NO ISSUE. */
    public SecretKey validPbkdf2Sha256(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 02. PBKDF2-HMAC-SHA224 - NO ISSUE.
     * SHA224 is explicitly allowed by this SecretKeyFactory rule.
     */
    public SecretKey validPbkdf2Sha224(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA224");

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 03. Allowlisted PBE factory - NO ISSUE.
     * The result is a PBE key, not a raw AES SecretKeySpec.
     * This fixture exercises key construction, not cipher initialization.
     */
    public SecretKey validPbeHmacSha512Aes256(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBEWithHmacSHA512AndAES_256");

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 210_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 04. getInstance(String, String) overload - NO ISSUE.
     * The named provider must be registered and support this algorithm.
     */
    public SecretKey validProviderName(
            char[] password, String providerName)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance(
                        "PBKDF2WithHmacSHA256", providerName);

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 05. getInstance(String, Provider) overload - NO ISSUE.
     * The Provider object need not be globally registered.
     */
    public SecretKey validProviderObject(
            char[] password, Provider provider)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance(
                        "PBKDF2WithHmacSHA512", provider);

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 210_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 06. Factory created in a helper and used in the caller - NO ISSUE.
     * Returning a factory is not inherently an incomplete operation.
     */
    public SecretKey validFactoryCreatedByHelper(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory = createSha256Factory();

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 07. Specification constructed in a helper and cleared in the caller -
     * NO ISSUE. Tests interprocedural speccedKey and lifecycle propagation.
     */
    public SecretKey validSpecCreatedByHelper(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec = createValidSha256Spec(password);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 08. Interface-typed specification and factory alias - NO ISSUE.
     * A 128-bit PBKDF2 result is allowed; the rule does not require 256 bits.
     */
    public SecretKey validKeySpecAndFactoryAliases(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKeyFactory alias = factory;

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 128);
        KeySpec interfaceReference = spec;

        try {
            return alias.generateSecret(interfaceReference);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 09. Translate an already-generated key using a separate factory -
     * NO ISSUE. Each factory performs exactly one modeled Gen event.
     */
    public SecretKey validTranslateOnSeparateFactory(char[] password)
            throws GeneralSecurityException {
        SecretKey original = deriveValidSha256Key(password);

        SecretKeyFactory translator =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return translator.translateKey(original);
    }

    /**
     * 10. Generate a key, then retrieve its PBEKeySpec - NO ISSUE.
     * getKeySpec() is not another Gen event in the pinned rule.
     * Both the original and exported password copies are cleared.
     */
    public int validGenerateThenGetKeySpec(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec originalSpec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);

        SecretKey key;
        try {
            key = factory.generateSecret(originalSpec);
        } finally {
            originalSpec.clearPassword();
        }

        PBEKeySpec exportedSpec =
                (PBEKeySpec) factory.getKeySpec(key, PBEKeySpec.class);
        try {
            return exportedSpec.getKeyLength();
        } finally {
            exportedSpec.clearPassword();
        }
    }

    /**
     * 11. New factory for each batch item - NO ISSUE.
     * An empty batch creates no factory, and every created factory performs
     * one generateSecret() call.
     */
    public SecretKey[] validNewFactoryPerBatchItem(char[][] passwords)
            throws GeneralSecurityException {
        SecretKey[] result = new SecretKey[passwords.length];

        for (int i = 0; i < passwords.length; i++) {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            PBEKeySpec spec =
                    new PBEKeySpec(
                            passwords[i], freshSalt(), 600_000, 256);
            try {
                result[i] = factory.generateSecret(spec);
            } finally {
                spec.clearPassword();
            }
        }

        return result;
    }

    // =========================================================================
    // SECTION 2: NON-ALLOWLISTED ALGORITHMS AND ALGORITHM DATAFLOW
    // =========================================================================

    /**
     * 12. PBKDF2-HMAC-SHA1 is excluded by the pinned allowlist.
     * This is a policy finding, not a claim that SHA-1 collisions directly
     * break HMAC-SHA1 or PBKDF2-HMAC-SHA1.
     */
    public SecretKey notAllowlistedPbkdf2Sha1(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); // CC: ALGORITHM/InvalidValue "PBKDF2WithHmacSHA1"

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /** 13. Legacy MD5/DES PBE factory - ALGORITHM ISSUE. */
    public SecretKey legacyPbeMd5Des(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES"); // CC: ALGORITHM/InvalidValue "PBEWithMD5AndDES"

        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 14. Conditional strong/legacy algorithm selection.
     * The diagnostic belongs at the consuming factory call.
     */
    public SecretKey conditionalLegacyKdf(
            char[] password, boolean legacyMode)
            throws GeneralSecurityException {
        String algorithm =
                legacyMode
                        ? "PBKDF2WithHmacSHA1"
                        : "PBKDF2WithHmacSHA256";

        SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm); // CC: ALGORITHM/InvalidValue "PBKDF2WithHmacSHA1"

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /** 15. Non-allowlisted algorithm returned by a helper. */
    public SecretKey algorithmReturnedByHelper(char[] password)
            throws GeneralSecurityException {
        String algorithm = legacyAlgorithmName();

        SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm); // CC: ALGORITHM/InvalidValue "PBKDF2WithHmacSHA1"

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 16. Weak algorithm stored in a field and consumed by the Provider
     * overload. Tests field dataflow and overloaded factory matching.
     */
    public SecretKey weakAlgorithmStoredInField(
            char[] password, Provider provider)
            throws GeneralSecurityException {
        this.fieldAlgorithm = "PBEWithMD5AndDES";

        SecretKeyFactory factory = SecretKeyFactory.getInstance(this.fieldAlgorithm, provider); // CC: ALGORITHM/InvalidValue "PBEWithMD5AndDES"

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 17. Downgrade when a selected provider lacks the strong algorithm.
     * The supplied provider may support SHA1 but not SHA256.
     */
    public SecretKey weakProviderFallback(
            char[] password, Provider provider)
            throws GeneralSecurityException {
        SecretKeyFactory factory;

        try {
            factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256", provider);
        } catch (NoSuchAlgorithmException unsupportedStrongAlgorithm) {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1", provider); // CC: ALGORITHM/InvalidValue "PBKDF2WithHmacSHA1"
        }

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    // =========================================================================
    // SECTION 3: SPECIFICATION, PASSWORD, SALT AND CLEANUP CASES
    // =========================================================================

    /**
     * 18. Externally supplied String converted to char[].
     * This targets neverTypeOf, not a hardcoded-password constraint.
     * Failure to establish speccedKey propagates to generateSecret().
     */
    public SecretKey passwordDerivedFromString(String password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        char[] characters = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(characters, freshSalt(), 600_000, 256); // CC: KEY_MATERIAL/ForbiddenType "java.lang.String"
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(characters, '\0');
        }
    }

    /**
     * 19. Literal char[] password: violates notHardCoded without involving
     * java.lang.String.
     *
     * The current converter falls back to GENERAL/Undefined for this
     * constraint type. The resulting missing speccedKey is a separate
     * finding at generateSecret().
     */
    public SecretKey hardcodedCharArrayPassword()
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        char[] password = new char[] {
                'p', 'a', 's', 's', 'w', 'o', 'r', 'd'
        };

        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), 600_000, 256); // CC: GENERAL/Undefined
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }

    /** 20. Fixed salt does not satisfy randomized[salt]. */
    public SecretKey hardcodedSalt(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] salt = new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16
        };

        PBEKeySpec spec = new PBEKeySpec(password, salt, 600_000, 256); // CC: KEY_MATERIAL/ImproperGenerated
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
        }
    }

    /** 21. java.util.Random salt does not satisfy randomized[salt]. */
    public SecretKey weakRandomSalt(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] salt = new byte[16];
        new Random().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, 600_000, 256); // CC: KEY_MATERIAL/ImproperGenerated
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 22. Salt has a secure source on one branch and a weak source on the
     * other. Findings are expected for the weak-source path.
     */
    public SecretKey mixedSaltSources(
            char[] password, boolean secureSource)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] salt = new byte[16];
        if (secureSource) {
            new SecureRandom().nextBytes(salt);
        } else {
            new Random().nextBytes(salt);
        }

        PBEKeySpec spec = new PBEKeySpec(password, salt, 600_000, 256); // CC: KEY_MATERIAL/ImproperGenerated
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 23. One iteration below the pinned minimum.
     * 9999 is legal Java, but violates iterationCount >= 10000.
     * The current comparison-constraint conversion is GENERAL/Undefined.
     */
    public SecretKey iterationBelowCryslMinimum(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), 9_999, 256); // CC: GENERAL/Undefined
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 24. Exact pinned iteration-count boundary - NO ISSUE.
     * This tests rule inclusivity, not a production work-factor recommendation.
     */
    public SecretKey exactCryslIterationMinimum(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 10_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 25. Password cleared before key generation.
     * clearPassword() negates speccedKey. Generation therefore lacks its
     * required predicate and will also fail with normal PBKDF2 providers.
     */
    public SecretKey clearedSpecBeforeGeneration(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        spec.clearPassword();

        return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
    }

    /**
     * 26. Specification replaced without clearing the discarded instance.
     * Only the replacement is used and cleared. The abandoned first object
     * must still produce its own cleanup diagnostic.
     */
    public SecretKey overwrittenSpecNotCleared(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), 600_000, 128); // CC: API_MISUSE/IncompleteOperation "javax.crypto.spec.PBEKeySpec"
        spec = new PBEKeySpec(password, freshSalt(), 600_000, 256);

        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 27. Password-only PBEKeySpec constructor is explicitly forbidden.
     * It does not establish speccedKey under the pinned rule.
     * It also lacks the parameters needed by a PBKDF2 provider.
     */
    public SecretKey forbiddenPasswordOnlySpec(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec = new PBEKeySpec(password); // CC: FORBIDDEN_METHOD/ForbiddenMethod
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 28. Three-argument PBEKeySpec constructor is explicitly forbidden.
     * Its key length is unspecified; PBKDF2 providers normally reject it.
     */
    public SecretKey forbiddenSpecWithoutKeyLength(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec = new PBEKeySpec(password, freshSalt(), 600_000); // CC: FORBIDDEN_METHOD/ForbiddenMethod
        try {
            return factory.generateSecret(spec); // CC: KEY_MATERIAL/ImproperGenerated
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 29. Incompatible concrete KeySpec - NO CRYSL FINDING EXPECTED.
     *
     * A securely constructed AES SecretKeySpec establishes speccedKey.
     * The factory rule checks that predicate, but does not require a
     * PBEKeySpec for PBKDF2.
     *
     * SunJCE rejects this with InvalidKeySpecException. This is an intentional
     * runtime-error fixture demonstrating a rule-coverage gap, not valid
     * PBKDF2 usage.
     */
    public SecretKey incompatibleConcreteKeySpec()
            throws GeneralSecurityException {
        byte[] material = new byte[32];
        new SecureRandom().nextBytes(material);
        SecretKeySpec aesSpec = new SecretKeySpec(material, "AES");

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(aesSpec);
    }

    // =========================================================================
    // SECTION 4: RULE-SPECIFIC LIFECYCLE / CONTROL-FLOW DIAGNOSTICS
    //
    // SecretKeyFactory is reusable in Java. The inspected rule nevertheless
    // permits exactly one generateSecret() OR translateKey() after creation.
    // It has no initialization or finalization method.
    // =========================================================================

    /**
     * 30. Factory used only for metadata.
     * Legal Java, but no Gen event occurs before the factory is abandoned.
     */
    public Provider metadataOnlyFactory()
            throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); // CC: API_MISUSE/IncompleteOperation "javax.crypto.SecretKeyFactory"
        return factory.getProvider();
    }

    /**
     * 31. Factory used only to extract a specification.
     * Legal Java. getKeySpec() does not satisfy Gen in the pinned rule.
     * The source key is generated by a different, completed factory.
     */
    public int retrievalOnlyFactory(char[] password)
            throws GeneralSecurityException {
        SecretKey key = deriveValidSha256Key(password);

        SecretKeyFactory extractor = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); // CC: API_MISUSE/IncompleteOperation "javax.crypto.SecretKeyFactory"

        PBEKeySpec exportedSpec =
                (PBEKeySpec) extractor.getKeySpec(key, PBEKeySpec.class);
        try {
            return exportedSpec.getKeyLength();
        } finally {
            exportedSpec.clearPassword();
        }
    }

    /**
     * 32. Generate two keys using one factory.
     * Legal Java; the second Gen event violates the pinned rule's order.
     */
    public SecretKey[] repeatedGenerateSecret(
            char[] firstPassword, char[] secondPassword)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec firstSpec =
                new PBEKeySpec(firstPassword, freshSalt(), 600_000, 256);
        PBEKeySpec secondSpec =
                new PBEKeySpec(secondPassword, freshSalt(), 600_000, 256);

        try {
            SecretKey first = factory.generateSecret(firstSpec);
            SecretKey second = factory.generateSecret(secondSpec); // CC: API_MISUSE/UnexpectedCall "SecretKeyFactory.generateSecret"
            return new SecretKey[] {first, second};
        } finally {
            firstSpec.clearPassword();
            secondSpec.clearPassword();
        }
    }

    /**
     * 33. Generate, then translate using the same factory.
     * Both calls are legal Java, but together constitute two Gen events.
     */
    public SecretKey generateThenTranslateOnSameFactory(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);

        SecretKey original;
        try {
            original = factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }

        return factory.translateKey(original); // CC: API_MISUSE/UnexpectedCall "SecretKeyFactory.translateKey"
    }

    /**
     * 34. Reusable factory outside a batch loop.
     *
     * Empty batch: no Gen event, so the rule reports incomplete operation.
     * One item: one Gen event, satisfying the rule.
     * Multiple items: later Gen events violate the rule.
     *
     * The reuse itself is legal Java.
     */
    public SecretKey[] reusableFactoryInBatchLoop(char[][] passwords)
            throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); // CC: API_MISUSE/IncompleteOperation "javax.crypto.SecretKeyFactory"

        SecretKey[] result = new SecretKey[passwords.length];
        for (int i = 0; i < passwords.length; i++) {
            PBEKeySpec spec =
                    new PBEKeySpec(
                            passwords[i], freshSalt(), 600_000, 256);
            try {
                result[i] = factory.generateSecret(spec); // CC: API_MISUSE/UnexpectedCall "SecretKeyFactory.generateSecret"
            } finally {
                spec.clearPassword();
            }
        }

        return result;
    }

    /**
     * 35. Early return abandons the factory on one feasible path.
     * The specification is constructed only after the abort check, avoiding
     * an unrelated PBEKeySpec cleanup diagnostic on the abort path.
     */
    public SecretKey earlyReturnBeforeGenerateSecret(
            char[] password, boolean abort)
            throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); // CC: API_MISUSE/IncompleteOperation "javax.crypto.SecretKeyFactory"

        if (abort) {
            return null;
        }

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * 36. Two independent factories; only one performs a Gen event.
     * Completing the used instance must not complete the unused instance.
     */
    public SecretKey completesOnlyOneOfTwoFactories(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory usedFactory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        SecretKeyFactory unusedFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); // CC: API_MISUSE/IncompleteOperation "javax.crypto.SecretKeyFactory"

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return usedFactory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }

    // =========================================================================
    // HELPERS: not additional numbered fixtures
    // =========================================================================

    private static byte[] freshSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static SecretKeyFactory createSha256Factory()
            throws NoSuchAlgorithmException {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    }

    private static PBEKeySpec createValidSha256Spec(char[] password) {
        return new PBEKeySpec(password, freshSalt(), 600_000, 256);
    }

    private static String legacyAlgorithmName() {
        return "PBKDF2WithHmacSHA1";
    }

    private static SecretKey deriveValidSha256Key(char[] password)
            throws GeneralSecurityException {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        PBEKeySpec spec =
                new PBEKeySpec(password, freshSalt(), 600_000, 256);
        try {
            return factory.generateSecret(spec);
        } finally {
            spec.clearPassword();
        }
    }
}