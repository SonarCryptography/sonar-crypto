package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.sonar.api.batch.sensor.issue.Issue;

/**
 * Unit tests for CcToSonarIssues class.
 *
 * Note: Tests for methods involving CogniCrypt types (WrappedClass, Method, AbstractError)
 * are difficult to create without proper mocking frameworks. These are tested through E2E tests instead.
 */
class CcToSonarIssuesTest {

  @TempDir Path tempDir;

  private SensorContextTester sensorContext;
  private CcToSonarIssues issueReporter;

  @BeforeEach
  void setUp() {
    sensorContext = SensorContextTester.create(tempDir);
    issueReporter = new CcToSonarIssues();
  }

  @Test
  void reportIssue_should_create_issue_with_correct_details() throws IOException {
    // Given: Create a test Java file
    Path srcDir = tempDir.resolve("src/main/java");
    Files.createDirectories(srcDir);
    Path javaFile = srcDir.resolve("com/example/TestClass.java");
    Files.createDirectories(javaFile.getParent());
    String fileContent = "package com.example;\n" +
                        "public class TestClass {\n" +
                        "  public void method() {\n" +
                        "    // Line 4\n" +
                        "  }\n" +
                        "}";
    Files.writeString(javaFile, fileContent);

    InputFile inputFile = TestInputFileBuilder.create(
            "test-module",
            tempDir.toFile(),
            javaFile.toFile()
        )
        .setLanguage("java")
        .setType(InputFile.Type.MAIN)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(fileContent)
        .build();

    sensorContext.fileSystem().add(inputFile);

    // When: Report an issue on line 3
    issueReporter.reportIssue(sensorContext, inputFile, 3, "Test error message");

    // Then: Issue should be created with correct details
    assertThat(sensorContext.allIssues()).hasSize(1);

    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.ruleKey()).hasToString(CryptoRulesDefinition.CC_RULE.toString());
    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(inputFile);
    assertThat(issue.primaryLocation().message()).isEqualTo("Cryptographic API misuse: Test error message");
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(3);
  }

  @Test
  void reportIssue_should_create_multiple_issues_on_same_file() throws IOException {
    // Given: Create a test Java file
    Path srcDir = tempDir.resolve("src/main/java");
    Files.createDirectories(srcDir);
    Path javaFile = srcDir.resolve("MultiIssue.java");
    Files.createDirectories(srcDir);
    String content = "public class MultiIssue {\n" +
                    "  void m1() {}\n" +
                    "  void m2() {}\n" +
                    "  void m3() {}\n" +
                    "}";
    Files.writeString(javaFile, content);

    InputFile inputFile = TestInputFileBuilder.create(
            "test-module",
            tempDir.toFile(),
            javaFile.toFile()
        )
        .setLanguage("java")
        .setType(InputFile.Type.MAIN)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();

    sensorContext.fileSystem().add(inputFile);

    // When: Report multiple issues on different lines
    issueReporter.reportIssue(sensorContext, inputFile, 2, "Error in method 1");
    issueReporter.reportIssue(sensorContext, inputFile, 3, "Error in method 2");
    issueReporter.reportIssue(sensorContext, inputFile, 4, "Error in method 3");

    // Then: Three separate issues should be created
    assertThat(sensorContext.allIssues()).hasSize(3);
  }

  @Test
  void reportIssue_should_prefix_message_with_api_misuse_text() throws IOException {
    // Given: Create a simple test file
    Path srcDir = tempDir.resolve("src/main/java");
    Files.createDirectories(srcDir);
    Path javaFile = srcDir.resolve("Test.java");
    String content = "public class Test {}";
    Files.writeString(javaFile, content);

    InputFile inputFile = TestInputFileBuilder.create(
            "test-module",
            tempDir.toFile(),
            javaFile.toFile()
        )
        .setLanguage("java")
        .setType(InputFile.Type.MAIN)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();

    sensorContext.fileSystem().add(inputFile);

    // When: Report an issue with a custom message
    String customMessage = "Weak encryption algorithm used";
    issueReporter.reportIssue(sensorContext, inputFile, 1, customMessage);

    // Then: The issue message should be prefixed
    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.primaryLocation().message())
        .startsWith("Cryptographic API misuse:")
        .contains(customMessage);
  }
}
