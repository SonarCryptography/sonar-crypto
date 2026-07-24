package com.example.crypto;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;

/**
 * This class contains intentional cryptographic vulnerabilities for testing
 * the SonarCrypto plugin. DO NOT use this code in production!
 */
public class WeakInterProcCryptoExamples {

    private Cipher createCipher() throws GeneralSecurityException {
        return Cipher.getInstance("AES/GCM/NoPadding");
    }
    
    private byte[] encrypt(Cipher cipher, byte[] data) throws GeneralSecurityException {
        return cipher.doFinal(data); // CC: API_MISUSE/UnexpectedCall "Cipher.doFinal"
    }

    public byte[] uninitializedCipherInstance(byte[] data) throws Exception {
        final var cipher = createCipher();
        return encrypt(cipher, data);
    }

}
