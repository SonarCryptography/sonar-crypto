package org.sonarcrypto.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

class FileUtilitiesTests {
  @RegisterExtension LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void find_sonar_crypto_jar() {
    File file =
        org.sonarcrypto.utility.FileUtilities.findFile(
            "../sonar-crypto-plugin/target", "sonar-crypto-plugin", ".jar");
    assertThat(file).isNotNull();
  }

  @Test
  void find_non_existent_fails() {
    File file = org.sonarcrypto.utility.FileUtilities.findFile("target", "non", ".existent");
    assertThat(logTester.logs(Level.ERROR))
        .containsExactly("Error (file not found) while searching for file: non*.existent");
    assertThat(file).isNull();
  }
}
