package com.example.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Arrays;

/**
 * Static-analysis fixtures for java.security.MessageDigest.
 *
 * Includes valid usages, weak algorithms, dataflow cases, and lifecycle
 * diagnostics expected under the pinned JCA CrySL rule.
 *
 * These are scanner fixtures, not JUnit tests or production examples.
 * Unless a case explicitly tests empty input, supplied inputs are assumed
 * non-null. Supplied providers must be non-null; named providers must exist.
 */
public class MessageDigestTests {

    private String selectedDigestAlgorithm;

    // =========================================================================
    // SECTION 1: VALID USAGES / FALSE-POSITIVE REGRESSION CASES
    // =========================================================================

    /** 01. SHA-256 single-shot hashing - NO ISSUE. */
    public byte[] validSha256SingleShot(byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input);
    }

    /** 02. SHA-384 incremental hashing of multiple parts - NO ISSUE. */
    public byte[] validSha384Incremental(byte[] first, byte[] second)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-384");
        md.update(first);
        md.update(second);
        return md.digest();
    }

    /**
     * 03. SHA-512 with a single-byte message-type prefix and a payload -
     * NO ISSUE. Covers update(byte) and update(byte[]).
     */
    public byte[] validSha512FramedInput(byte[] payload)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update((byte) 0x01);
        md.update(payload);
        return md.digest();
    }

    /**
     * 04. Explicit UTF-8 encoding and digest comparison - NO ISSUE.
     * The hardcoded text is public test data, not a hardcoded secret key.
     */
    public boolean validUtf8TestVectorComparison(byte[] expectedDigest)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] actualDigest =
                md.digest("abc".getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(actualDigest, expectedDigest);
    }

    /**
     * 05. Hashing a bounded slice of a framed input - NO ISSUE.
     * Literal bounds satisfy the rule's offset/length constraints.
     */
    public byte[] validInputSlice() throws NoSuchAlgorithmException {
        byte[] packet = new byte[] {
                10, 11, 12, 13, 14, 15, 16, 17
        };

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(packet, 2, 4);
        return md.digest();
    }

    /**
     * 06. Read-only heap ByteBuffer input - NO ISSUE.
     * update(ByteBuffer) does not require a writable backing array.
     */
    public byte[] validReadOnlyHeapByteBuffer(byte[] input)
            throws NoSuchAlgorithmException {
        ByteBuffer buffer = ByteBuffer.wrap(input).asReadOnlyBuffer();

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(buffer);
        return md.digest();
    }

    /**
     * 07. Direct ByteBuffer input, flipped before reading - NO ISSUE.
     * Covers a buffer without an accessible backing byte array.
     */
    public byte[] validDirectByteBuffer(byte[] input)
            throws NoSuchAlgorithmException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(input.length);
        buffer.put(input);
        buffer.flip();

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(buffer);
        return md.digest();
    }

    /**
     * 08. Output-buffer digest with a nonzero offset - NO ISSUE.
     * SHA-256 writes 32 bytes into output[8..39].
     */
    public byte[] validOutputBufferWithOffset(byte[] input)
            throws NoSuchAlgorithmException, DigestException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(input);

        byte[] output = new byte[40];
        md.digest(output, 8, 32);
        return Arrays.copyOfRange(output, 8, 40);
    }

    /**
     * 09. update(header), followed by digest(payload) - NO ISSUE.
     * digest(byte[]) appends its input; it does not discard prior updates.
     */
    public byte[] validUpdateThenDigestInput(byte[] header, byte[] payload)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(header);
        return md.digest(payload);
    }

    /**
     * 10. Reuse across three independent messages - NO ISSUE.
     * Every successful digest() resets the digest automatically.
     */
    public byte[][] validReuseAcrossMessages(
            byte[] first, byte[] second, byte[] third)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        byte[] firstHash = md.digest(first);

        md.update(second);
        byte[] secondHash = md.digest();

        byte[] thirdHash = md.digest(third);

        return new byte[][] {firstHash, secondHash, thirdHash};
    }

    /**
     * 11. Explicitly discard partial input and restart - NO ISSUE.
     * reset() is legal JCA usage; the actual message is finalized.
     */
    public byte[] validResetAndRestart(
            byte[] discardedInput, byte[] actualInput)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(discardedInput);
        md.reset();

        md.update(actualInput);
        return md.digest();
    }

    /**
     * 12. Single-shot hashing of an empty message - NO ISSUE.
     * digest(byte[]) is permitted directly after getInstance() by the rule.
     */
    public byte[] validEmptySingleShot() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(new byte[0]);
    }

    /**
     * 13. Whole-array update with an empty array, then digest() - NO ISSUE.
     * This overload has no explicit length argument constrained to be > 0.
     */
    public byte[] validEmptyWholeArrayUpdate()
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(new byte[0]);
        return md.digest();
    }

    /** 14. getInstance(String, String) provider overload - NO ISSUE. */
    public byte[] validProviderNameOverload(
            byte[] input, String providerName)
            throws GeneralSecurityException {
        MessageDigest md =
                MessageDigest.getInstance("SHA-256", providerName);
        return md.digest(input);
    }

    /**
     * 15. getInstance(String, Provider) overload - NO ISSUE.
     * A Provider object does not need to be globally registered.
     */
    public byte[] validProviderObjectOverload(
            byte[] input, Provider provider)
            throws NoSuchAlgorithmException {
        MessageDigest md =
                MessageDigest.getInstance("SHA-512", provider);
        return md.digest(input);
    }

    /**
     * 16. Update through an alias, finalize through the original reference -
     * NO ISSUE. Both references identify the same digest instance.
     */
    public byte[] validAliases(byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        MessageDigest alias = md;

        alias.update(input);
        return md.digest();
    }

    /**
     * 17. Factory helper returns an instance finalized by the caller -
     * NO ISSUE. Returning an unfinished instance is not inherently misuse.
     */
    public byte[] validFactoryHelper(byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest md = createSha256Digest();
        return md.digest(input);
    }

    /**
     * 18. Caller updates the digest; a helper finalizes it - NO ISSUE.
     * Tests interprocedural lifecycle tracking.
     */
    public byte[] validFinalizerHelper(byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(input);
        return finishDigest(md);
    }

    /**
     * 19. Both branches perform an update before a shared digest() -
     * NO ISSUE.
     */
    public byte[] validBothBranchesUpdated(
            byte[] first, byte[] second, boolean useFirst)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        if (useFirst) {
            md.update(first);
        } else {
            md.update(second);
        }

        return md.digest();
    }

    /**
     * 20. Clone a partially updated digest to hash two different suffixes -
     * NO ISSUE. Both computations are finalized.
     * Cloning is provider-dependent, hence CloneNotSupportedException.
     */
    public byte[][] validCloneWithSharedPrefix(
            byte[] prefix, byte[] leftSuffix, byte[] rightSuffix)
            throws NoSuchAlgorithmException, CloneNotSupportedException {
        MessageDigest original = MessageDigest.getInstance("SHA-256");
        original.update(prefix);

        MessageDigest copy = (MessageDigest) original.clone();

        original.update(leftSuffix);
        copy.update(rightSuffix);

        byte[] leftHash = original.digest();
        byte[] rightHash = copy.digest();

        return new byte[][] {leftHash, rightHash};
    }

    // =========================================================================
    // SECTION 2: WEAK ALGORITHMS AND ALGORITHM DATAFLOW
    // =========================================================================

    /** 21. Direct MD5 usage - WEAK ALGORITHM. */
    public byte[] weakMd5(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5"); // CC: ALGORITHM/InvalidValue "MD5"
        return md.digest(input);
    }

    /** 22. Direct SHA-1 usage using the standard algorithm name. */
    public byte[] weakSha1(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1"); // CC: ALGORITHM/InvalidValue "SHA-1"
        return md.digest(input);
    }

    /**
     * 23. SHA-1 using the commonly supported SHA1 alias.
     * Also matches the spelling used in WeakCryptoExamples.java.
     */
    public byte[] weakSha1Alias(byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1"); // CC: ALGORITHM/InvalidValue "SHA1"
        return md.digest(input);
    }

    /**
     * 24. A legacy branch selects MD5; the other branch selects SHA-256.
     * The diagnostic belongs on the consuming factory call.
     */
    public byte[] conditionalWeakAlgorithm(
            byte[] input, boolean legacyMode)
            throws NoSuchAlgorithmException {
        String algorithm = legacyMode ? "MD5" : "SHA-256";

        MessageDigest md = MessageDigest.getInstance(algorithm); // CC: ALGORITHM/InvalidValue "MD5"
        return md.digest(input);
    }

    /**
     * 25. Weak algorithm passed into a helper.
     * The expected diagnostic is inside digestUsingAlgorithm(), where the
     * value reaches MessageDigest.getInstance(), not on this helper call.
     */
    public byte[] weakAlgorithmThroughHelper(byte[] input)
            throws NoSuchAlgorithmException {
        return digestUsingAlgorithm("MD5", input);
    }

    /**
     * 26. Weak algorithm stored in a field and consumed by the Provider
     * overload. Tests field dataflow and the overloaded factory constraint.
     */
    public byte[] weakAlgorithmThroughFieldAndProvider(
            byte[] input, Provider provider)
            throws NoSuchAlgorithmException {
        this.selectedDigestAlgorithm = "MD5";

        MessageDigest md = MessageDigest.getInstance(this.selectedDigestAlgorithm, provider); // CC: ALGORITHM/InvalidValue "MD5"
        return md.digest(input);
    }

    /**
     * 27. Downgrade to MD5 when a selected provider lacks SHA-256.
     * A particular Provider is not required to implement every algorithm.
     */
    public byte[] weakProviderFallback(byte[] input, Provider provider)
            throws NoSuchAlgorithmException {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-256", provider);
        } catch (NoSuchAlgorithmException unsupportedStrongAlgorithm) {
            md = MessageDigest.getInstance("MD5", provider); // CC: ALGORITHM/InvalidValue "MD5"
        }

        return md.digest(input);
    }

    // =========================================================================
    // SECTION 3: RULE-EXPECTED LIFECYCLE DIAGNOSTICS
    //
    // The pinned rule uses:
    // Get, (digest(byte[]) | (Update+, Digest))+
    //
    // In particular, digest() and digest(output, offset, len) require a
    // preceding update event. Java itself permits hashing an empty message.
    // =========================================================================

    /**
     * 28. Zero-argument digest() immediately after creation.
     * LEGAL JCA empty hashing, but rejected by the inspected CrySL order.
     */
    public byte[] noUpdatesBeforeDigest()
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(); // CC: API_MISUSE/UnexpectedCall "MessageDigest.digest"
    }

    /**
     * 29. Output-buffer digest without a preceding update.
     * Buffer bounds are valid; the expected issue is solely rule order.
     */
    public byte[] noUpdatesBeforeOutputDigest()
            throws NoSuchAlgorithmException, DigestException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] output = new byte[32];

        md.digest(output, 0, 32); // CC: API_MISUSE/UnexpectedCall "MessageDigest.digest"
        return output;
    }

    /**
     * 30. A second zero-argument digest() without new input.
     * Java hashes the empty message on the second call; the rule rejects it.
     */
    public byte[] secondDigestWithoutNewUpdate(byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.digest(input);

        return md.digest(); // CC: API_MISUSE/UnexpectedCall "MessageDigest.digest"
    }

    /**
     * 31. Locally created instance is abandoned without a digest operation.
     * No instance escapes to a caller that could complete it.
     */
    public void incompleteAfterCreation()
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256"); // CC: API_MISUSE/IncompleteOperation "java.security.MessageDigest"
    }

    /** 32. Input is updated, but finalization is forgotten. */
    public void incompleteAfterUpdate(byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(input); // CC: API_MISUSE/IncompleteOperation "java.security.MessageDigest"
    }

    /**
     * 33. First operation completes; a second operation is left unfinished.
     * A previous successful digest does not finalize later updates.
     */
    public byte[] incompleteReuse(byte[] first, byte[] second)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] firstHash = md.digest(first);

        md.update(second); // CC: API_MISUSE/IncompleteOperation "java.security.MessageDigest"
        return firstHash;
    }

    /**
     * 34. An update occurs on only one branch.
     * The false branch legally hashes empty input in Java, but violates the
     * inspected rule's requirement for a preceding update event.
     */
    public byte[] conditionallyMissingUpdate(
            byte[] input, boolean includeInput)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        if (includeInput) {
            md.update(input);
        }

        return md.digest(); // CC: API_MISUSE/UnexpectedCall "MessageDigest.digest"
    }

    /**
     * 35. Chunked hashing with a loop that may execute zero times.
     * Expected rule diagnostic on the empty-chunks path.
     * The chunks array and its entries are assumed non-null.
     */
    public byte[] zeroIterationUpdateLoop(byte[][] chunks)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        for (byte[] chunk : chunks) {
            md.update(chunk);
        }

        return md.digest(); // CC: API_MISUSE/UnexpectedCall "MessageDigest.digest"
    }

    /**
     * 36. Early return abandons an updated digest on one feasible path.
     * The normal path finalizes the same instance.
     */
    public byte[] earlyReturnAfterUpdate(byte[] input, boolean abort)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(input); // CC: API_MISUSE/IncompleteOperation "java.security.MessageDigest"

        if (abort) {
            return new byte[0];
        }

        return md.digest();
    }

    /**
     * 37. Two independent instances are updated, but only one is finalized.
     * Tests object-sensitive lifecycle tracking: completing the first
     * instance must not complete the second instance.
     */
    public byte[] finalizesWrongInstance(byte[] first, byte[] second)
            throws NoSuchAlgorithmException {
        MessageDigest completed = MessageDigest.getInstance("SHA-256");
        MessageDigest forgotten = MessageDigest.getInstance("SHA-256");

        completed.update(first);
        forgotten.update(second); // CC: API_MISUSE/IncompleteOperation "java.security.MessageDigest"

        return completed.digest();
    }

    // =========================================================================
    // HELPERS: not additional numbered fixtures
    // =========================================================================

    private static MessageDigest createSha256Digest()
            throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    private static byte[] finishDigest(MessageDigest md) {
        return md.digest();
    }

    /**
     * Called with "MD5" by fixture 25.
     * The annotation is on the actual JCA factory invocation.
     */
    private static byte[] digestUsingAlgorithm(
            String algorithm, byte[] input)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm); // CC: ALGORITHM/InvalidValue "MD5"
        return md.digest(input);
    }
}