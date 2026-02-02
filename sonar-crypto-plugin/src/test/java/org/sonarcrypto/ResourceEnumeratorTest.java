package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonarcrypto.utils.ResourceEnumerator;

import java.io.IOException;
import java.nio.file.Path;

public class ResourceEnumeratorTest {

    @Test
    void ListResourcesTest () throws IOException {
        var list = ResourceEnumerator.listResources(Path.of("crysl_rules"), ".zip", s -> true);
        assertThat(list).isNotEmpty();
        assertThat(list).contains(
            Path.of("/crysl_rules/bc.zip"),
            Path.of("/crysl_rules/bc-jca.zip"),
            Path.of("/crysl_rules/jca.zip"),
            Path.of("/crysl_rules/tink.zip")
        );
    }
}
