package org.sonarcrypto.utils.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;

public class ResourceExtractorTest {

  @Test
  void extractTest() throws Exception {
    final var ruleset = "test_rules1";
    final var fileEnding = ".zip";
    final var rulesFolderName = "crysl_rules";
    final var tempDir = Files.createTempDirectory(rulesFolderName);

    final var extractedRulePaths =
        ResourceExtractor.extract(rulesFolderName, tempDir, fileEnding, ruleset::equalsIgnoreCase);

    assertThat(extractedRulePaths).isNotEmpty();

    final var entry = extractedRulePaths.get(0).getFileName();
    assertThat(entry.toString()).endsWith("test_rules1.zip");
  }
}
