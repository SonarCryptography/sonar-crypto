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
    var rulesetPaths = provider.extractRulesetToTempDir(Ruleset.BC);
    var myZip = new File(rulesetPaths.rulesetZip().toUri());
    assertThat(myZip.exists());
    assertThat(myZip.isFile());

    try (var zip = new ZipFile(myZip)) {
      var dirEntry = zip.getEntry("BouncyCastle");
      assertThat(dirEntry.isDirectory());

      var entry = zip.getEntry("BouncyCastle/RSAEngine.crysl");
      assertThat(entry).as("File should exist").isNotNull();
    }

    assertThat(rulesetPaths.dependencyClasspath())
        .as("BC ruleset should include bcprov-jdk18on on the classpath")
        .contains("bcprov-jdk18on");
  }

  @Test
  void testDependenciesExtracted() throws Exception {
    var provider = new CryslRuleProvider();

    // BC-JCA also depends on bcprov-jdk18on
    assertThat(provider.extractRulesetToTempDir(Ruleset.BC_JCA).dependencyClasspath())
        .contains("bcprov-jdk18on");

    // Tink depends on tink.jar
    assertThat(provider.extractRulesetToTempDir(Ruleset.TINK).dependencyClasspath())
        .contains("tink");

    // JCA only has javax.servlet-api as `provided` — no JARs should be copied
    assertThat(provider.extractRulesetToTempDir(Ruleset.JCA).dependencyClasspath())
        .as("JCA ruleset has no compile-scoped library deps")
        .isEmpty();
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
