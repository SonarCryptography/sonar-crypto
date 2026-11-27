package com.example.crypto;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;

/**
 * Static-analysis fixtures for java.security.Signature.
 *
 * These are source fixtures, not a runnable JUnit suite.
 * DO NOT use the intentionally insecure or runtime-error cases in production.
 *
 * Expectations target Crypto-API-Rules revision:
 * c946670c5d7bff23d8c5bdd9706bf14a29283646
 *
 * Allowed Signature algorithms:
 * - SHA256withRSA, SHA384withRSA, SHA512withRSA
 * - SHA256withECDSA, SHA384withECDSA, SHA512withECDSA
 * - SHA256withDSA
 *
 * Key-generation helpers satisfy the pinned KeyPairGenerator rule:
 * - RSA: 3072 bits
 * - EC: 256 bits
 * - DSA: 3072 bits
 *
 * "NO ISSUE" means no finding expected under these CrySL rules. It does
 * not guarantee that an entire application protocol is secure or correct.
 *
 * General assumptions:
 * - Message and signature arguments are non-null.
 * - Certificate arguments have a compatible public key and valid key usage.
 * - Unproven-key fixtures deliberately provide no generatedPrivkey or
 *   generatedPubkey predicate. This is missing analyzer provenance, not
 *   proof that the supplied key is cryptographically weak.
 * - Provider-specific cases assume OpenJDK's SunRsaSign is installed.
 *
 * Rule/API distinctions:
 * - Signing requires at least one Update event after initialization.
 * - Verification allows zero Update events.
 * - Consecutive Sign and Verify events are permitted.
 * - Successful sign()/verify() calls reset accumulated message data.
 * - Switching signing/verification modes is legal JCA but outside this rule.
 * - No cleanup method is required.
 * - Certificate trust and use of the verification result are not modeled.
 * - The SecureRandom argument of initSign is ignored by the Signature rule;
 *   SecureRandom itself is checked independently.
 * - Range constraints require offset >= 0 and len > 0.
 * - The modeled bounds comparison uses length[input], including when
 *   offset/len come from buffered sign or sliced verify.
 *
 * Runtime-error cases include otherwise complete source-level call
 * sequences so that the intended API/rule distinction remains explicit.
 *
 * Annotations identify intended primary diagnostics. Analyzer-specific
 * locations, path sensitivity, and additional diagnostics need scanning.
 */
public class SignatureTests {

    // =========================================================================
    // VALID USAGES: ALGORITHMS AND OVERLOADS
    // =========================================================================

    /** 01. Ordinary RSA/SHA-256 signing and verification - NO ISSUE. */
    public boolean test01RsaSha256RoundTrip(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message);
        byte[] encodedSignature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(message);
        return verifier.verify(encodedSignature);
    }

    /**
     * 02. Explicit provider name and single-byte updates - NO ISSUE.
     * This signs exactly two bytes.
     */
    public byte[] test02RsaSha384ProviderNameAndByteUpdates()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer =
                Signature.getInstance("SHA384withRSA", "SunRsaSign");
        signer.initSign(pair.getPrivate());
        signer.update((byte) 0x01);
        signer.update((byte) 0x02);
        return signer.sign();
    }

    /**
     * 03. Provider object, ranged update, buffered sign, sliced verify -
     * NO ISSUE.
     *
     * A 3072-bit RSA signature occupies 384 bytes. The message array is
     * deliberately large enough to satisfy the rule's input-based bounds
     * comparison for every ranged call.
     */
    public boolean test03RsaSha512AllArrayRangeOverloads()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        Provider provider = Security.getProvider("SunRsaSign");

        byte[] message = new byte[512];
        Arrays.fill(message, (byte) 0x42);
        byte[] output = new byte[400];

        Signature signer =
                Signature.getInstance("SHA512withRSA", provider);
        signer.initSign(pair.getPrivate());
        signer.update(message, 8, 384);
        int written = signer.sign(output, 8, 384);

        if (written != 384) {
            throw new GeneralSecurityException(
                    "Expected a 384-byte signature for the 3072-bit RSA key");
        }

        Signature verifier =
                Signature.getInstance("SHA512withRSA", provider);
        verifier.initVerify(pair.getPublic());
        verifier.update(message, 8, 384);
        return verifier.verify(output, 8, 384);
    }

    /**
     * 04. ECDSA/SHA-256, explicit randomness, direct/read-only ByteBuffers -
     * NO ISSUE.
     *
     * update(ByteBuffer) consumes position through limit. The read-only
     * buffer has an independent position and is created before signing
     * consumes the original buffer.
     */
    public boolean test04EcdsaSha256DirectAndReadOnlyBuffers()
            throws GeneralSecurityException {
        KeyPair pair = newEcKeyPair();

        ByteBuffer signingInput = ByteBuffer.allocateDirect(8);
        signingInput.put(new byte[] {0, 1, 2, 3, 4, 5, 6, 7});
        signingInput.flip();
        signingInput.position(2);
        signingInput.limit(6);

        ByteBuffer verificationInput = signingInput.asReadOnlyBuffer();

        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(pair.getPrivate(), new SecureRandom());
        signer.update(signingInput);
        byte[] encodedSignature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(verificationInput);
        boolean valid = verifier.verify(encodedSignature);

        return valid
                && signingInput.position() == signingInput.limit()
                && verificationInput.position() == verificationInput.limit();
    }

    /**
     * 05. ECDSA/SHA-384 with mixed byte, array and heap-buffer updates -
     * NO ISSUE. Updates concatenate data in call order.
     */
    public byte[] test05EcdsaSha384MixedUpdates(byte[] payload)
            throws GeneralSecurityException {
        KeyPair pair = newEcKeyPair();

        Signature signer = Signature.getInstance("SHA384withECDSA");
        signer.initSign(pair.getPrivate());
        signer.update((byte) 0x01);
        signer.update(payload);
        signer.update(ByteBuffer.wrap(new byte[] {0x02, 0x03}));
        return signer.sign();
    }

    /**
     * 06. ECDSA/SHA-512 created and initialized in a helper, completed by
     * the caller - NO ISSUE. Exercises interprocedural lifecycle propagation.
     */
    public byte[] test06EcdsaSha512SignerReturnedByHelper(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newEcKeyPair();

        Signature signer = newSha512EcdsaSigner(pair.getPrivate());
        signer.update(message);
        return signer.sign();
    }

    /**
     * 07. DSA/SHA-256 with a rule-approved 3072-bit key - NO ISSUE.
     * A valid signature is accepted; the same signature over modified data
     * is rejected. A false verification result is not a CrySL violation.
     */
    public boolean test07DsaSha256AcceptsOriginalRejectsModified()
            throws GeneralSecurityException {
        KeyPair pair = newDsaKeyPair();
        byte[] original = new byte[] {10, 20, 30};
        byte[] modified = new byte[] {10, 20, 31};

        Signature signer = Signature.getInstance("SHA256withDSA");
        signer.initSign(pair.getPrivate());
        signer.update(original);
        byte[] encodedSignature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withDSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(original);
        boolean originalAccepted = verifier.verify(encodedSignature);

        verifier.update(modified);
        boolean modifiedAccepted = verifier.verify(encodedSignature);

        return originalAccepted && !modifiedAccepted;
    }

    /**
     * 08. Certificate-based verification - NO SIGNATURE RULE ISSUE.
     *
     * Assume an externally trusted certificate with an appropriate RSA key.
     * This overload is permitted, but the Signature rule does not require
     * a trust predicate on the certificate or bind its publicKey variable.
     * Signature.initVerify(Certificate) does not validate a trust chain.
     */
    public boolean test08VerifyWithCertificate(
            Certificate certificate,
            byte[] message,
            byte[] encodedSignature)
            throws GeneralSecurityException {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(certificate);
        verifier.update(message);
        return verifier.verify(encodedSignature);
    }

    // =========================================================================
    // VALID EDGE CASES AND COMPLEX FLOWS
    // =========================================================================

    /**
     * 09. Empty-message signing with an explicit array Update event, followed
     * by verification with no update - NO ISSUE.
     *
     * The rule imposes no minimum length on update(byte[]).
     * Its verification branch permits Update*.
     */
    public boolean test09EmptyMessageWithExplicitSigningUpdate()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(new byte[0]);
        byte[] emptyMessageSignature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        return verifier.verify(emptyMessageSignature);
    }

    /**
     * 10. Consecutive sign and verify calls - NO CRYSL ISSUE.
     *
     * Sign+ and Verify+ permit consecutive terminal calls.
     * The first operation processes the supplied message; the second
     * operation processes the empty message because JCA resets the data.
     */
    public boolean test10ConsecutiveTerminalCallsProcessEmptySecondMessage()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[] message = new byte[] {1, 2, 3};

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message);
        byte[] firstSignature = signer.sign();
        byte[] emptyMessageSignature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(message);
        boolean firstValid = verifier.verify(firstSignature);
        boolean emptyValid = verifier.verify(emptyMessageSignature);

        return firstValid && emptyValid;
    }

    /**
     * 11. Reusing separate signer/verifier instances in a loop - NO ISSUE.
     *
     * A do-while loop guarantees at least one completed operation.
     * No reinitialization is needed when the keys remain unchanged.
     */
    public boolean test11RepeatedRoundTripsInGuaranteedNonemptyLoop()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[][] messages = {
                new byte[] {1, 2},
                new byte[] {3, 4, 5},
                new byte[] {6}
        };

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());

        boolean allValid = true;
        int index = 0;
        do {
            signer.update(messages[index]);
            byte[] encodedSignature = signer.sign();

            verifier.update(messages[index]);
            boolean valid = verifier.verify(encodedSignature);
            allValid = allValid && valid;
            index++;
        } while (index < messages.length);

        return allValid;
    }

    /**
     * 12. Repeated same-mode initialization and key rotation - NO ISSUE.
     *
     * InitSign+ permits repeated initialization before any update.
     * Reinitialization after a completed signature starts another group.
     * Returns signatures for the first and second key, in that order.
     */
    public byte[][] test12SameModeReinitializationAndKeyRotation(byte[] message)
            throws GeneralSecurityException {
        KeyPair firstPair = newRsaKeyPair();
        KeyPair secondPair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(firstPair.getPrivate());
        signer.initSign(secondPair.getPrivate());
        signer.update(message);
        byte[] signatureForSecondKey = signer.sign();

        signer.initSign(firstPair.getPrivate());
        signer.update(message);
        byte[] signatureForFirstKey = signer.sign();

        return new byte[][] {
                signatureForFirstKey,
                signatureForSecondKey
        };
    }

    /**
     * 13. Local alias with updates on both branches - NO ISSUE.
     * Every branch performs an update on the same initialized object.
     */
    public byte[] test13AliasAndCompleteBranching(boolean firstBranch)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature original = Signature.getInstance("SHA256withRSA");
        Signature alias = original;
        alias.initSign(pair.getPrivate());

        if (firstBranch) {
            original.update(new byte[] {1, 2});
        } else {
            alias.update(new byte[] {3, 4});
        }

        return original.sign();
    }

    /**
     * 14. Initialized by the caller, updated and signed by a helper -
     * NO ISSUE. Exercises the opposite flow direction to case 06.
     */
    public byte[] test14SigningCompletedByHelper(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        return updateAndSign(signer, message);
    }

    /**
     * 15. Signature stored in an object field and completed across helpers -
     * NO ISSUE. The holder is local to this fixture invocation.
     */
    public byte[] test15FieldBasedInterproceduralSigning(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        SignatureHolder holder = new SignatureHolder();

        holder.signature = Signature.getInstance("SHA256withRSA");
        holder.signature.initSign(pair.getPrivate());

        updateStoredSignature(holder, message);
        return finishStoredSignature(holder);
    }

    // =========================================================================
    // ALGORITHM ALLOWLIST FINDINGS
    // =========================================================================

    /** 16. RSA with SHA-1 - WEAK ALGORITHM / RULE VIOLATION. */
    public byte[] test16LegacySha1WithRsa(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA1withRSA"); // CC: ALGORITHM/InvalidValue "SHA1withRSA"
        signer.initSign(pair.getPrivate());
        signer.update(message);
        return signer.sign();
    }

    /** 17. RSA with MD5 - WEAK ALGORITHM / RULE VIOLATION. */
    public byte[] test17Md5WithRsa(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("MD5withRSA"); // CC: ALGORITHM/InvalidValue "MD5withRSA"
        signer.initSign(pair.getPrivate());
        signer.update(message);
        return signer.sign();
    }

    /**
     * 18. Raw RSA without an integrated digest - RULE VIOLATION.
     * A short fixed message avoids the raw RSA input-size limit.
     */
    public byte[] test18RawRsaWithoutDigest()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("NONEwithRSA"); // CC: ALGORITHM/InvalidValue "NONEwithRSA"
        signer.initSign(pair.getPrivate());
        signer.update(new byte[] {1, 2, 3});
        return signer.sign();
    }

    /** 19. ECDSA with SHA-1 - WEAK ALGORITHM / RULE VIOLATION. */
    public byte[] test19LegacySha1WithEcdsa(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newEcKeyPair();

        Signature signer = Signature.getInstance("SHA1withECDSA"); // CC: ALGORITHM/InvalidValue "SHA1withECDSA"
        signer.initSign(pair.getPrivate());
        signer.update(message);
        return signer.sign();
    }

    /**
     * 20. Properly parameterized RSA-PSS - ALLOWLIST VIOLATION ONLY.
     *
     * RSASSA-PSS is not inherently insecure. The pinned Signature rule
     * excludes it and does not model setParameter().
     * No PSSParameterSpec rule exists in this pinned JCA rules directory.
     */
    public byte[] test20RsaPssExcludedByPinnedAllowlist(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        PSSParameterSpec parameters = new PSSParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                32,
                PSSParameterSpec.TRAILER_FIELD_BC);

        Signature signer = Signature.getInstance("RSASSA-PSS"); // CC: ALGORITHM/InvalidValue "RSASSA-PSS"
        signer.setParameter(parameters);
        signer.initSign(pair.getPrivate());
        signer.update(message);
        return signer.sign();
    }

    /**
     * 21. Ed25519 verification - ALLOWLIST VIOLATION ONLY.
     *
     * Assume a compatible Ed25519 certificate and encoded signature.
     * The certificate overload avoids introducing an additional
     * KeyPairGenerator allowlist finding for Ed25519 key generation.
     * Ed25519 is not inherently insecure.
     */
    public boolean test21Ed25519ExcludedByPinnedAllowlist(
            Certificate ed25519Certificate,
            byte[] message,
            byte[] encodedSignature)
            throws GeneralSecurityException {
        Signature verifier = Signature.getInstance("Ed25519"); // CC: ALGORITHM/InvalidValue "Ed25519"
        verifier.initVerify(ed25519Certificate);
        verifier.update(message);
        return verifier.verify(encodedSignature);
    }

    /**
     * 22. Algorithm selected on a branch - RULE VIOLATION on the legacy path.
     * Both paths complete the lifecycle; only SHA1withRSA is disallowed.
     */
    public byte[] test22MixedAllowedAndWeakAlgorithm(
            byte[] message, boolean legacy)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        String algorithm = legacy ? "SHA1withRSA" : "SHA256withRSA";

        Signature signer = Signature.getInstance(algorithm); // CC: ALGORITHM/InvalidValue "SHA1withRSA"
        signer.initSign(pair.getPrivate());
        signer.update(message);
        return signer.sign();
    }

    // =========================================================================
    // KEY PROVENANCE AND RANDOMNESS
    // =========================================================================

    /**
     * 23. Private key without generatedPrivkey evidence -
     * REQUIRED-PREDICATE FINDING.
     *
     * Assume a runtime-compatible RSA key. Lack of analyzer provenance
     * is not equivalent to an intrinsically weak key.
     */
    public byte[] test23UnprovenPrivateKey(
            PrivateKey unprovenKey, byte[] message)
            throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(unprovenKey); // CC: KEY_MATERIAL/ImproperGenerated
        signer.update(message);
        return signer.sign();
    }

    /**
     * 24. Public key without generatedPubkey evidence -
     * REQUIRED-PREDICATE FINDING.
     *
     * Assume a runtime-compatible RSA key. The rule requires provenance
     * even though public keys are not secret material.
     */
    public boolean test24UnprovenPublicKey(
            PublicKey unprovenKey,
            byte[] message,
            byte[] encodedSignature)
            throws GeneralSecurityException {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(unprovenKey); // CC: KEY_MATERIAL/ImproperGenerated
        verifier.update(message);
        return verifier.verify(encodedSignature);
    }

    /**
     * 25. Predictably seeded randomness for ECDSA -
     * SECURERANDOM RULE FINDING.
     *
     * Signature.initSign(PrivateKey, SecureRandom) has no randomness
     * requirement in the pinned Signature rule. The seed finding belongs
     * on SecureRandom.setSeed(), not on Signature.initSign().
     */
    public byte[] test25EcdsaWithPredictablySeededRandom(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newEcKeyPair();

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(12345L); // CC: KEY_MATERIAL/ImproperGenerated

        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(pair.getPrivate(), random);
        signer.update(message);
        return signer.sign();
    }

    // =========================================================================
    // INCOMPLETE OPERATIONS
    // =========================================================================

    /** 26. Factory result abandoned before initialization - RULE VIOLATION. */
    public void test26SignatureCreatedButNeverUsed()
            throws GeneralSecurityException {
        Signature unused = Signature.getInstance("SHA256withRSA"); // CC: API_MISUSE/IncompleteOperation "java.security.Signature"
    }

    /** 27. Signing initialization without update/sign - RULE VIOLATION. */
    public void test27SigningInitializedButNeverCompleted()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate()); // CC: API_MISUSE/IncompleteOperation "java.security.Signature"
    }

    /** 28. Verification initialization without verify - RULE VIOLATION. */
    public void test28VerificationInitializedButNeverCompleted()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic()); // CC: API_MISUSE/IncompleteOperation "java.security.Signature"
    }

    /** 29. Updated signing operation without sign - RULE VIOLATION. */
    public void test29SigningUpdatedButNeverFinalized(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message); // CC: API_MISUSE/IncompleteOperation "java.security.Signature"
    }

    /** 30. Updated verification operation without verify - RULE VIOLATION. */
    public void test30VerificationUpdatedButNeverFinalized(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(message); // CC: API_MISUSE/IncompleteOperation "java.security.Signature"
    }

    // =========================================================================
    // UNEXPECTED CALLS AND RULE-POLICY LIFECYCLE DISTINCTIONS
    // =========================================================================

    /**
     * 31. update before initialization - RULE VIOLATION.
     * JCA also throws SignatureException.
     */
    public byte[] test31UpdateBeforeInitialization(byte[] message)
            throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.update(message); // CC: API_MISUSE/UnexpectedCall "Signature.update"
        return signer.sign();
    }

    /**
     * 32. sign before initialization - RULE VIOLATION.
     * JCA also throws SignatureException.
     */
    public byte[] test32SignBeforeInitialization()
            throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        return signer.sign(); // CC: API_MISUSE/UnexpectedCall "Signature.sign"
    }

    /**
     * 33. verify before initialization - RULE VIOLATION.
     * JCA checks the mode before processing the supplied signature.
     */
    public boolean test33VerifyBeforeInitialization()
            throws GeneralSecurityException {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        return verifier.verify(new byte[384]); // CC: API_MISUSE/UnexpectedCall "Signature.verify"
    }

    /**
     * 34. sign while initialized for verification - RULE VIOLATION.
     * JCA also throws SignatureException.
     */
    public byte[] test34SignInVerificationMode(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(pair.getPublic());
        signature.update(message);
        return signature.sign(); // CC: API_MISUSE/UnexpectedCall "Signature.sign"
    }

    /**
     * 35. verify while initialized for signing - RULE VIOLATION.
     * JCA also throws SignatureException.
     */
    public boolean test35VerifyInSigningMode(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(pair.getPrivate());
        signature.update(message);
        return signature.verify(new byte[384]); // CC: API_MISUSE/UnexpectedCall "Signature.verify"
    }

    /**
     * 36. First sign without an Update event - RULE-POLICY VIOLATION.
     *
     * JCA can sign the empty message directly after initSign().
     * The pinned rule nevertheless requires Update+.
     * Case 09 shows its accepted explicit-empty-update equivalent.
     */
    public byte[] test36FirstSignWithoutUpdate()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        return signer.sign(); // CC: API_MISUSE/UnexpectedCall "Signature.sign"
    }

    /**
     * 37. Signing followed by verification on the same instance -
     * RULE-POLICY VIOLATION.
     *
     * Legal JCA. The rule chooses a signing or verification branch for
     * the instance and does not permit switching between them.
     */
    public boolean test37SwitchFromSigningToVerification(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(pair.getPrivate());
        signature.update(message);
        byte[] encodedSignature = signature.sign();

        signature.initVerify(pair.getPublic()); // CC: API_MISUSE/UnexpectedCall "Signature.initVerify"
        signature.update(message);
        return signature.verify(encodedSignature);
    }

    /**
     * 38. Verification followed by signing on the same instance -
     * RULE-POLICY VIOLATION. This direction is also legal JCA.
     */
    public byte[] test38SwitchFromVerificationToSigning(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[] encodedSignature = rsaSignature(pair, message);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(pair.getPublic());
        signature.update(message);
        boolean valid = signature.verify(encodedSignature);

        if (!valid) {
            throw new GeneralSecurityException(
                    "Fixture expected the matching signature to verify");
        }

        signature.initSign(pair.getPrivate()); // CC: API_MISUSE/UnexpectedCall "Signature.initSign"
        signature.update(message);
        return signature.sign();
    }

    /**
     * 39. Same-mode reinitialization after update but before sign -
     * RULE-POLICY VIOLATION.
     *
     * Legal JCA: reinitialization discards the pending message.
     * Unlike case 12, this interrupts an unfinished Update+/Sign+ group.
     */
    public byte[] test39ReinitializeWhileMessageIsPending()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        PrivateKey privateKey = pair.getPrivate();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(new byte[] {1, 2, 3});

        signer.initSign(privateKey); // CC: API_MISUSE/UnexpectedCall "Signature.initSign"
        signer.update(new byte[] {4, 5, 6});
        return signer.sign();
    }

    // =========================================================================
    // CONTROL-FLOW AND OBJECT-IDENTITY FAILURES
    // =========================================================================

    /**
     * 40. Early return abandons the updated operation on one feasible path -
     * INCOMPLETE-OPERATION FINDING.
     */
    public byte[] test40EarlyReturnSkipsSigning(
            byte[] message, boolean abort)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message); // CC: API_MISUSE/IncompleteOperation "java.security.Signature"

        if (abort) {
            return null;
        }

        return signer.sign();
    }

    /**
     * 41. Overwriting the reference abandons the first instance -
     * INCOMPLETE-OPERATION FINDING.
     *
     * Completing the replacement instance does not complete the original.
     */
    public byte[] test41OverwrittenUnfinishedInstance(byte[] message)
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        PrivateKey privateKey = pair.getPrivate();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(message); // CC: API_MISUSE/IncompleteOperation "java.security.Signature"

        signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(message);
        return signer.sign();
    }

    // =========================================================================
    // ARRAY-RANGE CONSTRAINT FINDINGS
    // =========================================================================

    /**
     * 42. Negative update offset - COMPARISON-CONSTRAINT FINDING.
     * The other comparisons pass: len is positive and offset + len is zero.
     * JCA throws IllegalArgumentException.
     */
    public byte[] test42NegativeUpdateOffset()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[] message = new byte[16];

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message, -1, 1); // CC: GENERAL/Undefined
        return signer.sign();
    }

    /**
     * 43. Zero-length ranged update - RULE-POLICY CONSTRAINT FINDING.
     *
     * JCA permits a zero-length update. The rule requires len > 0.
     * This differs from update(new byte[0]), which case 09 permits.
     */
    public byte[] test43ZeroLengthRangedUpdate()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[] message = new byte[16];

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message, 0, 0); // CC: GENERAL/Undefined
        return signer.sign();
    }

    /**
     * 44. offset + len exceeds the message length -
     * COMPARISON-CONSTRAINT FINDING.
     * JCA throws IllegalArgumentException.
     */
    public byte[] test44UpdateRangeExceedsInput()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[] message = new byte[16];

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message, 8, 9); // CC: GENERAL/Undefined
        return signer.sign();
    }

    // =========================================================================
    // API ERRORS AND SECURITY LOGIC NOT MODELED BY THE SIGNATURE RULE
    // =========================================================================

    /**
     * 45. Generated EC private key supplied to an RSA Signature -
     * NO CRYSL ISSUE EXPECTED, but a JCA key-compatibility error.
     *
     * generatedPrivkey is satisfied. The Signature rule does not relate
     * the key's algorithm to its selected signature algorithm.
     */
    public byte[] test45RsaSignatureWithEcPrivateKey(byte[] message)
            throws GeneralSecurityException {
        KeyPair ecPair = newEcKeyPair();

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(ecPair.getPrivate()); // API ONLY: throws InvalidKeyException.
        signer.update(message);
        return signer.sign();
    }

    /**
     * 46. Output capacity below the actual RSA signature size -
     * NO CRYSL ISSUE EXPECTED, but a JCA output-capacity error.
     *
     * The rule sees input length 512 >= offset + len 16.
     * It does not require enough output space for the 384-byte signature.
     */
    public int test46OutputTooSmallForActualSignature()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[] message = new byte[512];
        byte[] output = new byte[16];

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message);
        return signer.sign(output, 0, 16); // API ONLY: throws SignatureException.
    }

    /**
     * 47. Signature slice exceeds the actual signature array -
     * NO CRYSL ISSUE EXPECTED, but a JCA bounds error.
     *
     * The rule compares message input length 512 against 8 + 384.
     * JCA checks the actual signature array, whose length is only 16.
     */
    public boolean test47VerifySliceExceedsSignatureArray()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[] message = new byte[512];
        byte[] encodedSignature = new byte[16];

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(message);
        return verifier.verify(encodedSignature, 8, 384); // API ONLY: throws IllegalArgumentException.
    }

    /**
     * 48. Verification result ignored - SECURITY BUG, NO CRYSL ISSUE EXPECTED.
     *
     * The signature belongs to the original message, not the modified one.
     * verify() returns false, but this method still reports acceptance.
     * The rule ensures a verified predicate on the result; it does not
     * require that the result be true or used to control acceptance.
     */
    public boolean test48IgnoresFailedVerification()
            throws GeneralSecurityException {
        KeyPair pair = newRsaKeyPair();
        byte[] original = new byte[] {1, 2, 3};
        byte[] modified = new byte[] {1, 2, 4};
        byte[] encodedSignature = rsaSignature(pair, original);

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(pair.getPublic());
        verifier.update(modified);
        verifier.verify(encodedSignature); // SECURITY BUG: false result ignored; no CC finding expected.
        return true;
    }

    // =========================================================================
    // HELPERS: NOT ADDITIONAL NUMBERED FIXTURES
    // =========================================================================

    private static KeyPair newRsaKeyPair()
            throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        return generator.generateKeyPair();
    }

    private static KeyPair newEcKeyPair()
            throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        return generator.generateKeyPair();
    }

    private static KeyPair newDsaKeyPair()
            throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("DSA");
        generator.initialize(3072);
        return generator.generateKeyPair();
    }

    private static Signature newSha512EcdsaSigner(PrivateKey privateKey)
            throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA512withECDSA");
        signer.initSign(privateKey);
        return signer;
    }

    private static byte[] updateAndSign(
            Signature signer, byte[] message)
            throws GeneralSecurityException {
        signer.update(message);
        return signer.sign();
    }

    private static byte[] rsaSignature(KeyPair pair, byte[] message)
            throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(message);
        return signer.sign();
    }

    private static void updateStoredSignature(
            SignatureHolder holder, byte[] message)
            throws GeneralSecurityException {
        holder.signature.update(message);
    }

    private static byte[] finishStoredSignature(SignatureHolder holder)
            throws GeneralSecurityException {
        return holder.signature.sign();
    }

    private static final class SignatureHolder {
        private Signature signature;
    }
}