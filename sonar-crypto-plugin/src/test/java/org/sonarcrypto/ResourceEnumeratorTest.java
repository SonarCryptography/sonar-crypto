package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonarcrypto.rules.CryslRuleProvider;
import org.sonarcrypto.utils.ResourceEnumerator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;

public class ResourceEnumeratorTest {

    @Test
    void ListResourcesTest () throws IOException, URISyntaxException {
        var path = Path.of("crysl_rules");

        var list = ResourceEnumerator.listResources(path, ".zip", new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return true;
            }
        });
        assertThat(list).hasSize(4);
        assertThat(list).isNotNull();
        assertThat(list).isNotEmpty();
        assertThat(list).contains(Path.of("/crysl_rules/bc.zip"), Path.of("/crysl_rules/bc-jca.zip"),
                Path.of("/crysl_rules/jca.zip"), Path.of("/crysl_rules/tink.zip"));
    }
}
