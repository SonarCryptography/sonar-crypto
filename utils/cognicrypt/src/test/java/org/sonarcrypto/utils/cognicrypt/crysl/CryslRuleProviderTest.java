package org.sonarcrypto.utils.cognicrypt.crysl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

public class CryslRuleProviderTest {

  @Test
  void testRulesFound() throws Exception {
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
  }

  @Test
  void testRuleNotFound() {
    var provider = new CryslRuleProvider();
    assertThatThrownBy(
            () -> {
              // The code that should crash
              provider.extractRulesetToTempDir("non_existing_rule_name");
            })
        .isInstanceOf(IOException.class);
  }
}
