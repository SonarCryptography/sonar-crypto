package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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
        assertThat(myZip.exists());
        assertThat(myZip.isFile());
        try (var zip = new ZipFile(myZip)) {
            var dirEntry = zip.getEntry("BouncyCastle");
            assertThat(dirEntry.isDirectory());
            var entry = zip.getEntry("BouncyCastle/RSAEngine.crysl");
            assertThat(entry).as("File should exist").isNotNull();
        }
        assertThat(new File(path.toUri()).exists());
        assertThat(new File(path.toUri()).isFile());
        System.out.println(path);

        assertThatThrownBy(() -> {
            // The code that should crash
            provider.extractRulesetToTempDir("");
        })
                .isInstanceOf(IOException.class);
    }
}
