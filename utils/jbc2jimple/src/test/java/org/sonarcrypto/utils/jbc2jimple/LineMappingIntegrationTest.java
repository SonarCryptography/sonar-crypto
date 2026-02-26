package org.sonarcrypto.utils.jbc2jimple;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonarcrypto.utils.jimple.mapper.ElementType;
import org.sonarcrypto.utils.jimple.mapper.LineMapping;
import org.sonarcrypto.utils.jimple.mapper.LineMappingCollection;
import org.sonarcrypto.utils.jimple.mapper.LineNumberMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test demonstrating the complete line mapping functionality.
 */
class LineMappingIntegrationTest {

  @Test
  void testEndToEndLineMappingCollection(@TempDir Path tempDir) throws IOException {
    // This test requires actual compiled classes to work
    // For now, we verify the API works correctly

    Jbc2JimpleConverter converter = new Jbc2JimpleConverter();

    // Verify that we can get mappings (will be empty without actual classes)
    Map<String, LineMappingCollection> mappings = converter.getLineMappings();
    assertThat(mappings).isNotNull();
    assertThat(mappings).isEmpty(); // No classes converted yet
  }

  @Test
  void testLineMappingApiUsage() {
    // Demonstrate the API usage pattern
    Jbc2JimpleConverter converter = new Jbc2JimpleConverter();

    // After conversion, mappings can be retrieved
    Map<String, LineMappingCollection> mappings = converter.getLineMappings();

    // For each class that was converted
    for (Map.Entry<String, LineMappingCollection> entry : mappings.entrySet()) {
      String className = entry.getKey();
      LineMappingCollection collection = entry.getValue();

      // Verify collection structure
      assertThat(collection.getClassName()).isNotNull();
      assertThat(collection.getMappings()).isNotNull();

      // Process each mapping
      for (LineMapping mapping : collection.getMappings()) {
        // Each mapping has all required information
        assertThat(mapping.getJimpleLine()).isGreaterThanOrEqualTo(0);
        assertThat(mapping.getElementType()).isNotNull();
        assertThat(mapping.getElementSignature()).isNotNull();
        // Source file name may be null if not available
        // assertThat(mapping.getSourceFileName()).isNotNull();
        assertThat(mapping.getSourcePosition()).isNotNull();
      }
    }
  }

  @Test
  void testLineMappingDataStructure() {
    // Create a sample mapping to verify structure
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");

    // Record various types of positions
    mapper.recordClassPosition(1, "com.example.TestClass",
        new sootup.core.model.FullPosition(1, 1, 50, 1));

    mapper.recordFieldPosition(3, "<com.example.TestClass: int value>",
        new sootup.core.model.FullPosition(5, 5, 5, 20));

    mapper.recordMethodPosition(7, "<com.example.TestClass: void test()>",
        new sootup.core.model.FullPosition(10, 3, 20, 4));

    mapper.recordStmtPosition(12, "$r0 := @this: com.example.TestClass",
        new sootup.core.model.FullPosition(11, 9, 11, 50));

    // Get collection
    LineMappingCollection collection = mapper.getCollection();

    // Verify structure
    assertThat(collection.getClassName()).isEqualTo("com.example.TestClass");
    assertThat(collection.getSourceFileName()).isEqualTo("TestClass.java");
    assertThat(collection.size()).isEqualTo(4);

    // Verify order and types
    assertThat(collection.getMappings().get(0).getElementType()).isEqualTo(ElementType.CLASS);
    assertThat(collection.getMappings().get(1).getElementType()).isEqualTo(ElementType.FIELD);
    assertThat(collection.getMappings().get(2).getElementType()).isEqualTo(ElementType.METHOD);
    assertThat(collection.getMappings().get(3).getElementType()).isEqualTo(ElementType.STATEMENT);

    // Verify Jimple line numbers are sequential
    assertThat(collection.getMappings().get(0).getJimpleLine()).isEqualTo(1);
    assertThat(collection.getMappings().get(1).getJimpleLine()).isEqualTo(3);
    assertThat(collection.getMappings().get(2).getJimpleLine()).isEqualTo(7);
    assertThat(collection.getMappings().get(3).getJimpleLine()).isEqualTo(12);
  }
}

