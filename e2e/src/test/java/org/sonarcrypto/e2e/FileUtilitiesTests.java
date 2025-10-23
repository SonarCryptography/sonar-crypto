package org.sonarcrypto.e2e;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class FileUtilitiesTests {
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void fails_to_find_jar_in_nonexistent_dir() {
    File jar = FileUtilities.sonarCryptoJar("../sonar-crypto-plugin/target/does/not/exist");
    assertThat(jar).isNull();
    assertThat(logTester.getLogs())
      .extracting(LogAndArguments::getRawMsg)
      .containsExactly("Build directory does not exist: {}");
  }

  @Test
  void fails_to_find_jar_in_invalid_dir() {
    File jar = FileUtilities.sonarCryptoJar("../sonar-crypto-plugin");
    assertThat(jar).isNull();
    assertThat(logTester.getLogs())
      .extracting(LogAndArguments::getRawMsg)
      .containsExactly("Could not find sonar-crypto-plugin jar in build directory: {}");
  }

  @Test
  void fails_to_find_jar_with_invalid_cononical_file() throws IOException {
    File mockedFile = spy(new File("../sonar-crypto-plugin"));
    when(mockedFile.getCanonicalFile()).thenThrow(new IOException("Mocked Exception"));
    File jar = FileUtilities.sonarCryptoJar(mockedFile);
    assertThat(jar).isNull();
    assertThat(logTester.getLogs())
      .extracting(LogAndArguments::getRawMsg)
      .containsExactly("Could not resolve canonical path for build directory: {}");
  }

  @Test
  void find_jar() {
    File jar = FileUtilities.sonarCryptoJar("../sonar-crypto-plugin/target");
    assertThat(jar).isNotNull();
  }
}
