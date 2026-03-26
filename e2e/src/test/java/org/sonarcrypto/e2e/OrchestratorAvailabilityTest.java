package org.sonarcrypto.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

public class OrchestratorAvailabilityTest {
  @Test
  void find_sq_for_orchestrator() {
    File file =
        org.sonarcrypto.utility.FileUtilities.findFile("target", "sq_for_orchestrator-", ".zip");
    assertThat(file).isNotNull();
  }
}
