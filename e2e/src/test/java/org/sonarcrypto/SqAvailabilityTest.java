package org.sonarcrypto;

import java.io.File;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqAvailabilityTest {
    @Test
    void zip_file_is_downloaded() {
        File sqZip = new File("target/SQ/sq_for_orchestrator.zip");
        assertThat(sqZip).exists();
    }
}
