package org.sonarcrypto.utils.jimple.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sootup.core.model.FullPosition;

class LineMappingJsonTest {

  @Test
  void testToJson() throws IOException {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");

    mapper.recordClassPosition(1, "com.example.TestClass", new FullPosition(10, 1, 50, 1));
    mapper.recordFieldPosition(
        3, "<com.example.TestClass: int value>", new FullPosition(15, 5, 15, 20));
    mapper.recordMethodPosition(
        7, "<com.example.TestClass: void test()>", new FullPosition(20, 3, 30, 4));

    LineMappingCollection collection = mapper.getCollection();
    String json = collection.toJson();

    // Verify JSON is valid and contains expected data
    assertThat(json).isNotNull();
    assertThat(json).contains("com.example.TestClass");
    assertThat(json).contains("TestClass.java");
    assertThat(json).contains("CLASS");
    assertThat(json).contains("FIELD");
    assertThat(json).contains("METHOD");

    // Parse JSON to verify structure
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode root = objectMapper.readTree(json);

    assertThat(root.get("className").asText()).isEqualTo("com.example.TestClass");
    assertThat(root.get("sourceFileName").asText()).isEqualTo("TestClass.java");
    assertThat(root.get("mappings")).isNotNull();
    assertThat(root.get("mappings").isArray()).isTrue();
    assertThat(root.get("mappings").size()).isEqualTo(3);

    // Verify first mapping (class) - should NOT have sourceFileName field
    JsonNode firstMapping = root.get("mappings").get(0);
    assertThat(firstMapping.get("jimpleLine").asInt()).isEqualTo(1);
    assertThat(firstMapping.get("elementType").asText()).isEqualTo("CLASS");
    assertThat(firstMapping.get("elementSignature").asText()).isEqualTo("com.example.TestClass");
    assertThat(firstMapping.get("sourceFileName")).isNull(); // Should not exist in mappings

    JsonNode sourcePos = firstMapping.get("sourcePosition");
    assertThat(sourcePos.get("firstLine").asInt()).isEqualTo(10);
    assertThat(sourcePos.get("lastLine").asInt()).isEqualTo(50);
    assertThat(sourcePos.get("firstCol").asInt()).isEqualTo(1);
    assertThat(sourcePos.get("lastCol").asInt()).isEqualTo(1);
  }

  @Test
  void testWriteJsonToWriter() throws IOException {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");
    mapper.recordClassPosition(1, "com.example.TestClass", new FullPosition(10, 1, 50, 1));

    LineMappingCollection collection = mapper.getCollection();

    StringWriter writer = new StringWriter();
    collection.writeJson(writer);
    String json = writer.toString();

    assertThat(json).isNotNull();
    assertThat(json).contains("com.example.TestClass");
    assertThat(json).contains("TestClass.java");
  }

  @Test
  void testWriteJsonToFile(@TempDir Path tempDir) throws IOException {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");

    mapper.recordClassPosition(1, "com.example.TestClass", new FullPosition(10, 1, 50, 1));
    mapper.recordFieldPosition(
        3, "<com.example.TestClass: int value>", new FullPosition(15, 5, 15, 20));

    LineMappingCollection collection = mapper.getCollection();

    // Write to file
    Path jsonFile = tempDir.resolve("test.jimple.map.json");
    try (var writer = Files.newBufferedWriter(jsonFile)) {
      collection.writeJson(writer);
    }

    // Verify file exists and contains valid JSON
    assertThat(Files.exists(jsonFile)).isTrue();
    String content = Files.readString(jsonFile);
    assertThat(content).isNotEmpty();

    // Parse and verify
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode root = objectMapper.readTree(content);
    assertThat(root.get("className").asText()).isEqualTo("com.example.TestClass");
    assertThat(root.get("sourceFileName").asText()).isEqualTo("TestClass.java");
    assertThat(root.get("mappings").size()).isEqualTo(2);
  }

  @Test
  void testJsonFormatIsPrettyPrinted() throws IOException {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");
    mapper.recordClassPosition(1, "com.example.TestClass", new FullPosition(10, 1, 50, 1));

    LineMappingCollection collection = mapper.getCollection();
    String json = collection.toJson();

    // Pretty printed JSON should contain newlines
    assertThat(json).contains("\n");
    assertThat(json).contains("  "); // Should have indentation
  }
}
