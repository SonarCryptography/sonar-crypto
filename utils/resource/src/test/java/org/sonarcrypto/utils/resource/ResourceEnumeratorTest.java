package org.sonarcrypto.utils.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;

public class ResourceEnumeratorTest {

  @Test
  void testEnumerateResourcesFromFiles() throws Exception {
    final var list =
        new ResourceEnumerator()
            .listResources(
                Path.of("crysl_rules"),
                ".zip",
                s -> "test_rules1".equals(s) || "test_rules2".equals(s));

    assertThat(list).hasSize(2);
    assertThat(list)
        .contains(Path.of("crysl_rules/test_rules1.zip"), Path.of("crysl_rules/test_rules2.zip"));
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
  void testEnumerateResourcesWithinAJarFromClasspath() throws Exception {
    final var jarFile = Files.createTempFile("resource-enumerator", ".jar");
    try (var outputStream = new JarOutputStream(Files.newOutputStream(jarFile))) {
      outputStream.putNextEntry(new JarEntry("jar_only_rules/"));
      outputStream.closeEntry();
      outputStream.putNextEntry(new JarEntry("jar_only_rules/sample.zip"));
      outputStream.write(new byte[] {1});
      outputStream.closeEntry();
    }

    final var originalClassPath = System.getProperty("java.class.path");
    System.setProperty(
        "java.class.path", originalClassPath + File.pathSeparator + jarFile.toAbsolutePath());

    try {
      final var list =
          new ResourceEnumerator()
              .listResources(Path.of("jar_only_rules"), ".zip", "sample"::equals);

      assertThat(list).contains(Path.of("jar_only_rules/sample.zip"));
    } finally {
      System.setProperty("java.class.path", originalClassPath);
      Files.deleteIfExists(jarFile);
    }
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
