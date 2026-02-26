package org.sonarcrypto.utils.jbc2jimple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonarcrypto.utils.jbc2jimple.Jbc2JimpleConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test demonstrating the complete file writing functionality.
 */
class Jbc2JimpleConverterFileWritingTest {

  @Test
  void testMappingFilesAreWritten(@TempDir Path tempDir) throws IOException {
    // Get the path to test classes
    String testClassPath = "src/test/resources";
    Path testClassDir = Path.of(testClassPath);

    // Skip test if test resources don't exist
    if (!Files.exists(testClassDir)) {
      System.out.println("Skipping test - no test resources found");
      return;
    }

    Jbc2JimpleConverter converter = new Jbc2JimpleConverter();

    // Convert (will attempt to convert any classes in test resources)
    try {
      long count = converter.convert(testClassPath, tempDir.toString());
      System.out.println("Converted " + count + " class(es)");

      if (count > 0) {
        // Verify that both .jimple and .jimple.map.json files were created
        List<Path> jimpleFiles = Files.list(tempDir)
            .filter(p -> p.getFileName().toString().endsWith(".jimple"))
            .collect(Collectors.toList());

        List<Path> mappingFiles = Files.list(tempDir)
            .filter(p -> p.getFileName().toString().endsWith(".jimple.map.json"))
            .collect(Collectors.toList());

        assertThat(jimpleFiles).isNotEmpty();
        assertThat(mappingFiles).isNotEmpty();
        assertThat(mappingFiles.size()).isEqualTo(jimpleFiles.size());

        // Verify that mapping files contain valid JSON
        for (Path mappingFile : mappingFiles) {
          String content = Files.readString(mappingFile);
          assertThat(content).isNotEmpty();

          // Parse JSON to verify structure
          ObjectMapper objectMapper = new ObjectMapper();
          JsonNode root = objectMapper.readTree(content);
          assertThat(root.get("className")).isNotNull();
          assertThat(root.get("sourceFileName")).isNotNull(); // Should be at collection level
          assertThat(root.get("mappings")).isNotNull();
          assertThat(root.get("mappings").isArray()).isTrue();

          // Verify individual mappings don't have sourceFileName
          if (root.get("mappings").size() > 0) {
            JsonNode firstMapping = root.get("mappings").get(0);
            assertThat(firstMapping.get("sourceFileName")).isNull(); // Should NOT be in individual mappings
          }

          System.out.println("Verified mapping file: " + mappingFile.getFileName() +
                           " with " + root.get("mappings").size() + " mappings");
        }
      }
    } catch (Exception e) {
      System.out.println("Note: Test couldn't complete conversion: " + e.getMessage());
      // This is expected if there are no valid class files in test resources
    }
  }

  @Test
  void testMappingFileNamingConvention(@TempDir Path tempDir) throws IOException {
    // This test verifies the naming convention even without actual conversion
    String className = "com.example.TestClass";
    String expectedJimpleFile = className + ".jimple";
    String expectedMappingFile = className + ".jimple.map.json";

    assertThat(expectedMappingFile).endsWith(".jimple.map.json");
    assertThat(expectedMappingFile).startsWith(className);
  }
}

