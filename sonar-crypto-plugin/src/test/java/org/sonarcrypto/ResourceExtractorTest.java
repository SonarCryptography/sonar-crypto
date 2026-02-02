package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonarcrypto.utils.ResourceExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourceExtractorTest {

    @Test
    void extractTest () throws IOException {
        final var ruleset = "bc-jca";
        final var fileEnding = ".zip";
        final var rulesFolderName = "crysl_rules";
        final var tempDir = Files.createTempDirectory(rulesFolderName);
        final var extractedRulePaths = ResourceExtractor.extract(
                rulesFolderName,
                tempDir,
                fileEnding,
                ruleset::equalsIgnoreCase
        );

        final var foundRules = extractedRulePaths.size();
        final var entry = extractedRulePaths.get(0).getFileName();

        assertThat(foundRules).isNotZero();
        assertThat(extractedRulePaths).isNotEmpty();
        assertThat(entry.toString()).endsWith("bc-jca.zip");
    }
}
