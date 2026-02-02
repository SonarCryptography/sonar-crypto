package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonarcrypto.utils.ResourceExtractor;

import java.io.IOException;
import java.nio.file.Files;

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
        assertThat(extractedRulePaths).isNotEmpty();

        final var entry = extractedRulePaths.get(0).getFileName();
        assertThat(entry.toString()).endsWith("bc-jca.zip");
    }
}
