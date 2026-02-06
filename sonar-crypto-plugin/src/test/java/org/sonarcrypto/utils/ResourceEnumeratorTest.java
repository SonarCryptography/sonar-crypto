package org.sonarcrypto.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ResourceEnumeratorTest {

  @Test
  void testEnumerateResourcesFromFiles() throws Exception {
    final var list =
        new ResourceEnumerator()
            .listResources(Path.of("crysl_rules"), ".zip", s -> "jca".equals(s) || "bc".equals(s));

    assertThat(list).hasSize(2);
    assertThat(list).contains(Path.of("crysl_rules/bc.zip"), Path.of("crysl_rules/jca.zip"));
  }

  @Test
  void testEnumerateResourcesWithinAJar() throws Exception {
    final var classLoader = ResourceEnumeratorTest.class.getClassLoader();
    final var resourceUrl = classLoader.getResource("testResourceEnumeration.jar");

    final var list =
        new ResourceEnumerator()
            .listJarResources(
                URI.create("jar:" + resourceUrl + "!/crysl_rules").toURL(),
                Path.of("crysl_rules"),
                ".zip",
                "jca"::equals);

    assertThat(list).isNotEmpty();
    assertThat(list).contains(Path.of("crysl_rules/jca.zip"));
  }

  @Test
  void testInvalidEntitiesThreshold() {
    assertThatThrownBy(() -> new ResourceEnumerator(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testExhaustedEntitiesThreshold() throws Exception {
    final var classLoader = ResourceEnumeratorTest.class.getClassLoader();
    final var resourceUrl = classLoader.getResource("testResourceEnumeration.jar");

    final var list =
        new ResourceEnumerator(1)
            .listJarResources(
                URI.create("jar:" + resourceUrl + "!/crysl_rules").toURL(),
                Path.of("crysl_rules"),
                ".zip",
                s -> true);

    assertThat(list.size()).isLessThanOrEqualTo(1);
  }
}
