package org.sonarcrypto.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class PrerequisitesAvailabilityTest {
  @Test
  void find_sq_for_orchestrator() {
    File file =
        org.sonarcrypto.utility.FileUtilities.findFile("target", "sq_for_orchestrator-", ".zip");
    assertThat(file).isNotNull();
  }

  @Test
  void find_sonar_crypto_jar() {
    File file =
        org.sonarcrypto.utility.FileUtilities.findFile(
            "../sonar-crypto-plugin/target", "sonar-crypto-plugin", ".jar");
    assertThat(file).isNotNull();
  }
}
