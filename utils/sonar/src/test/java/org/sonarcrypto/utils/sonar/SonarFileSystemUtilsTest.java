package org.sonarcrypto.utils.sonar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.sonarcrypto.utils.sonar.SonarFileSystemUtils.findInputFile;

import boomerang.scope.WrappedClass;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

public class SonarFileSystemUtilsTest {
  @TempDir Path tempDir;
  private SensorContextTester sensorContext;

  @BeforeEach
  void setUp() {
    sensorContext = SensorContextTester.create(tempDir);
  }

  @Test
  void find_input_file_returns_file_for_matching_class() throws IOException {
    addJavaFile("com/example/MyClass.java", "package com.example;\npublic class MyClass {}");

    InputFile result =
        findInputFile(sensorContext.fileSystem(), wrappedClass("com.example.MyClass"));

    assertThat(result).isNotNull();
    assertThat(result.filename()).isEqualTo("MyClass.java");
  }

  @Test
  void find_input_file_returns_null_when_no_match() {
    InputFile result =
        findInputFile(sensorContext.fileSystem(), wrappedClass("com.example.NonExistent"));

    assertThat(result).isNull();
  }

  @Test
  void find_input_file_handles_deeply_nested_packages() throws IOException {
    addJavaFile(
        "com/example/crypto/utils/Helper.java",
        "package com.example.crypto.utils;\npublic class Helper {}");

    InputFile result =
        findInputFile(sensorContext.fileSystem(), wrappedClass("com.example.crypto.utils.Helper"));

    assertThat(result).isNotNull();
    assertThat(result.filename()).isEqualTo("Helper.java");
  }

  @Test
  void find_input_file_ignores_test_files() throws IOException {
    // Add a test file (not MAIN type)
    Path srcDir = tempDir.resolve("src/test/java/com/example");
    Files.createDirectories(srcDir);
    Path javaFile = srcDir.resolve("TestClass.java");
    String content = "package com.example;\npublic class TestClass {}";
    Files.writeString(javaFile, content);

    InputFile testFile =
        TestInputFileBuilder.create("mod", tempDir.toFile(), javaFile.toFile())
            .setLanguage("java")
            .setType(InputFile.Type.TEST)
            .setCharset(StandardCharsets.UTF_8)
            .setContents(content)
            .build();
    sensorContext.fileSystem().add(testFile);

    InputFile result =
        findInputFile(sensorContext.fileSystem(), wrappedClass("com.example.TestClass"));

    assertThat(result).isNull();
  }

  private static WrappedClass wrappedClass(String fqn) {
    WrappedClass wc = mock(WrappedClass.class);
    when(wc.getFullyQualifiedName()).thenReturn(fqn);
    return wc;
  }

  private void addJavaFile(String relativePath, String content) throws IOException {
    Path srcDir = tempDir.resolve("src/main/java");
    Files.createDirectories(srcDir);
    Path javaFile = srcDir.resolve(relativePath);
    Files.createDirectories(javaFile.getParent());
    Files.writeString(javaFile, content);

    InputFile inputFile =
        TestInputFileBuilder.create("mod", tempDir.toFile(), javaFile.toFile())
            .setLanguage("java")
            .setType(InputFile.Type.MAIN)
            .setCharset(StandardCharsets.UTF_8)
            .setContents(content)
            .build();
    sensorContext.fileSystem().add(inputFile);
  }
}
