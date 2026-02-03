package org.sonarcrypto.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class ResourceEnumeratorTest {
	
	@Test
	void testEnumerateResourcesFromFiles() throws IOException {
		final var list = ResourceEnumerator.listResources(Path.of("crysl_rules"), ".zip", s -> true);
		assertThat(list).isNotEmpty();
		assertThat(list).contains(
			Path.of("crysl_rules/bc.zip"),
			Path.of("crysl_rules/bc-jca.zip"),
			Path.of("crysl_rules/jca.zip"),
			Path.of("crysl_rules/tink.zip")
		);
	}
	
	@Test
	void testEnumerateResourcesWithinAJar() throws IOException {
		final var classLoader = ResourceEnumeratorTest.class.getClassLoader();
		final var resourceUrl = classLoader.getResource("testResourceEnumeration.jar");
		
		final var list = ResourceEnumerator.listJarResources(
			URI.create("jar:" + resourceUrl + "!/crysl_rules").toURL(),
			Path.of("crysl_rules"), ".zip",
			s -> true
		);
		
		assertThat(list).isNotEmpty();
		assertThat(list).contains(Path.of("crysl_rules/jca.zip"));
	}
}
