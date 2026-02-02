package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonarcrypto.rules.CryslRuleProvider;


import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class CryslRuleProviderTest {

    @Test
    void cryslRuleProviderTest () throws IOException {
        var provider = new CryslRuleProvider();
        var path = provider.extractRulesetToTempDir("bc");
        var myZip = new File(path.toUri());
        try (var zip = new ZipFile(myZip)) {
            var dirEntry = zip.getEntry("BouncyCastle");
            var entry = zip.getEntry("BouncyCastle/RSAEngine.crysl");
            assertThat(entry).as("File should exist").isNotNull();
            assertThat(dirEntry.isDirectory());
        }
        assertThat(new File(path.toUri()).exists());
        assertThat(new File(path.toUri()).isFile());
        System.out.println(path);
    }
}
