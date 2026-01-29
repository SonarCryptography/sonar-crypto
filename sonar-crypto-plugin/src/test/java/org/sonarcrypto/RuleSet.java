package org.sonarcrypto;

public enum RuleSet {

    BOUNCY_CASTLE("BouncyCastle"),
    BOUNCY_CASTLE_JCA("BouncyCastle-JCA"),
    JAVA_CRYPTO("JavaCryptographicArchitecture"),
    TINK("Tink");

    public final String dirName;

    RuleSet(String dirName) {
        this.dirName = dirName;
    }


}
