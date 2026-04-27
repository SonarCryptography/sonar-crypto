package org.sonarcrypto.utils.jimple.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    assertThat(json)
        .isNotNull()
        .contains("com.example.TestClass")
        .contains("TestClass.java")
        .contains("CLASS")
        .contains("FIELD")
        .contains("METHOD");

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
  void testDeserializeNewFormat() throws IOException {
    // The new format has an absolute sourceFileName, plus optional lhsPosition and argumentMappings
    String json =
        """
        {
          "className": "com.example.crypto.WeakCryptoExamples",
          "sourceFileName": "/path/to/WeakCryptoExamples.java",
          "mappings": [
            {
              "jimpleLine": 17,
              "elementType": "STATEMENT",
              "elementSignature": "_0 = staticinvoke <javax.crypto.Cipher: javax.crypto.Cipher getInstance(java.lang.String)>(\\"DES\\")",
              "sourcePosition": { "firstLine": 31, "lastLine": 31, "firstCol": 24, "lastCol": 49 },
              "lhsPosition": { "firstLine": 31, "lastLine": 31, "firstCol": 24, "lastCol": 36 },
              "argumentMappings": [
                { "argIndex": 1, "sourcePosition": { "firstLine": 31, "lastLine": 31, "firstCol": 43, "lastCol": 48 } }
              ]
            },
            {
              "jimpleLine": 18,
              "elementType": "STATEMENT",
              "elementSignature": "cipher = _0",
              "sourcePosition": { "firstLine": 31, "lastLine": 31, "firstCol": 8, "lastCol": 50 }
            }
          ]
        }
        """;

    ObjectMapper objectMapper = new ObjectMapper();
    LineMappingCollection collection = objectMapper.readValue(json, LineMappingCollection.class);

    assertThat(collection.getClassName()).isEqualTo("com.example.crypto.WeakCryptoExamples");
    assertThat(collection.getSourceFileName()).isEqualTo("/path/to/WeakCryptoExamples.java");
    assertThat(collection.size()).isEqualTo(2);

    // First mapping has lhsPosition and argumentMappings
    LineMapping first = collection.getMappings().get(0);
    assertThat(first.getJimpleLine()).isEqualTo(17);
    assertThat(first.getSourcePosition().getFirstLine()).isEqualTo(31);
    assertThat(first.getSourcePosition().getFirstCol()).isEqualTo(24);
    assertThat(first.getSourcePosition().getLastCol()).isEqualTo(49);
    assertThat(first.getLhsPosition()).isNotNull();
    assertThat(first.getLhsPosition().getFirstCol()).isEqualTo(24);
    assertThat(first.getLhsPosition().getLastCol()).isEqualTo(36);
    assertThat(first.getArgumentMappings()).isNotNull();
    List<ArgumentMapping> argMappings = first.getArgumentMappings();
    assertThat(argMappings).hasSize(1);
    assertThat(argMappings.get(0).getArgIndex()).isEqualTo(1);
    assertThat(argMappings.get(0).getSourcePosition().getFirstCol()).isEqualTo(43);

    // Second mapping has no lhsPosition or argumentMappings
    LineMapping second = collection.getMappings().get(1);
    assertThat(second.getJimpleLine()).isEqualTo(18);
    assertThat(second.getLhsPosition()).isNull();
    assertThat(second.getArgumentMappings()).isNull();
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
