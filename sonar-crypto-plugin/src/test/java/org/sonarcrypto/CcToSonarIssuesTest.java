package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import boomerang.scope.WrappedClass;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

class CcToSonarIssuesTest {

  @RegisterExtension LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @TempDir Path tempDir;

  private SensorContextTester sensorContext;
  private CcToSonarIssues issueReporter;

  @BeforeEach
  void setUp() {
    sensorContext = SensorContextTester.create(tempDir);
    issueReporter = new CcToSonarIssues();
  }

  // --- findInputFile tests ---

  @Test
  void find_input_file_returns_file_for_matching_class() throws IOException {
    addJavaFile("com/example/MyClass.java", "package com.example;\npublic class MyClass {}");

    InputFile result =
        issueReporter.findInputFile(
            sensorContext.fileSystem(), wrappedClass("com.example.MyClass"));

    assertThat(result).isNotNull();
    assertThat(result.filename()).isEqualTo("MyClass.java");
  }

  @Test
  void find_input_file_returns_null_when_no_match() {
    InputFile result =
        issueReporter.findInputFile(
            sensorContext.fileSystem(), wrappedClass("com.example.NonExistent"));

    assertThat(result).isNull();
  }

  @Test
  void find_input_file_handles_deeply_nested_packages() throws IOException {
    addJavaFile(
        "com/example/crypto/utils/Helper.java",
        "package com.example.crypto.utils;\npublic class Helper {}");

    InputFile result =
        issueReporter.findInputFile(
            sensorContext.fileSystem(), wrappedClass("com.example.crypto.utils.Helper"));

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
        issueReporter.findInputFile(
            sensorContext.fileSystem(), wrappedClass("com.example.TestClass"));

    assertThat(result).isNull();
  }

  // --- reportAllIssues tests ---

  @Test
  void report_all_issues_creates_issues_for_found_files() throws IOException {
    addJavaFile("com/example/ClassA.java", "package com.example;\npublic class ClassA {}");
    addJavaFile("com/example/ClassB.java", "package com.example;\npublic class ClassB {}");

    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> table = HashBasedTable.create();
    table.put(
        wrappedClass("com.example.ClassA"), method("doStuff"), Set.of(mock(AbstractError.class)));
    table.put(
        wrappedClass("com.example.ClassB"), method("encrypt"), Set.of(mock(AbstractError.class)));

    issueReporter.reportAllIssues(sensorContext, table);

    assertThat(sensorContext.allIssues()).hasSize(2);
  }

  @Test
  void report_all_issues_skips_classes_without_source_files() {
    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> table = HashBasedTable.create();
    table.put(wrappedClass("com.example.Missing"), method("m"), Set.of(mock(AbstractError.class)));

    issueReporter.reportAllIssues(sensorContext, table);

    assertThat(sensorContext.allIssues()).isEmpty();
    assertThat(logTester.logs())
        .anyMatch(log -> log.contains("Could not find source file for class: com.example.Missing"));
  }

  @Test
  void report_all_issues_reports_errors_from_multiple_methods() throws IOException {
    addJavaFile("com/example/Buggy.java", "package com.example;\npublic class Buggy {}");

    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> table = HashBasedTable.create();
    WrappedClass clazz = wrappedClass("com.example.Buggy");
    table.put(clazz, method("init"), Set.of(mock(AbstractError.class)));
    table.put(clazz, method("encrypt"), Set.of(mock(AbstractError.class)));
    table.put(clazz, method("decrypt"), Set.of(mock(AbstractError.class)));

    issueReporter.reportAllIssues(sensorContext, table);

    assertThat(sensorContext.allIssues()).hasSize(3);
  }

  @Test
  void report_all_issues_includes_method_name_in_message() throws IOException {
    addJavaFile("com/example/Foo.java", "package com.example;\npublic class Foo {}");

    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> table = HashBasedTable.create();
    table.put(
        wrappedClass("com.example.Foo"), method("encrypt"), Set.of(mock(AbstractError.class)));

    issueReporter.reportAllIssues(sensorContext, table);

    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.primaryLocation().message()).contains("encrypt");
  }

  @Test
  void report_all_issues_handles_empty_table() {
    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> table = HashBasedTable.create();

    issueReporter.reportAllIssues(sensorContext, table);

    assertThat(sensorContext.allIssues()).isEmpty();
  }

  // --- reportIssue tests ---

  @Test
  void report_issue_should_create_issue_with_correct_details() throws IOException {
    InputFile inputFile =
        addJavaFile(
            "com/example/TestClass.java",
            """
                package com.example;
                public class TestClass {
                  public void method() {
                    // Line 4
                  }
                }
                """);

    issueReporter.reportIssue(sensorContext, inputFile, 3, "Test error message");

    assertThat(sensorContext.allIssues()).hasSize(1);

    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.ruleKey()).hasToString(CryptoRulesDefinition.CC_RULE.toString());
    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(inputFile);
    assertThat(issue.primaryLocation().message())
        .isEqualTo("Cryptographic API misuse: Test error message");
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(3);
  }

  @Test
  void report_issue_should_create_multiple_issues_on_same_file() throws IOException {
    InputFile inputFile =
        addJavaFile(
            "MultiIssue.java",
            """
                public class MultiIssue {
                  void m1() {}
                  void m2() {}
                  void m3() {}
                }
                """);

    issueReporter.reportIssue(sensorContext, inputFile, 2, "Error in method 1");
    issueReporter.reportIssue(sensorContext, inputFile, 3, "Error in method 2");
    issueReporter.reportIssue(sensorContext, inputFile, 4, "Error in method 3");

    assertThat(sensorContext.allIssues()).hasSize(3);
  }

  @Test
  void report_issue_should_prefix_message_with_api_misuse_text() throws IOException {
    InputFile inputFile = addJavaFile("Test.java", "public class Test {}");

    String customMessage = "Weak encryption algorithm used";
    issueReporter.reportIssue(sensorContext, inputFile, 1, customMessage);

    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.primaryLocation().message())
        .startsWith("Cryptographic API misuse:")
        .contains(customMessage);
  }

  // --- Test helpers ---

  private InputFile addJavaFile(String relativePath, String content) throws IOException {
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
    return inputFile;
  }

  private static WrappedClass wrappedClass(String fqn) {
    WrappedClass wc = mock(WrappedClass.class);
    when(wc.getFullyQualifiedName()).thenReturn(fqn);
    return wc;
  }

  private static boomerang.scope.Method method(String name) {
    boomerang.scope.Method m = mock(boomerang.scope.Method.class);
    when(m.getName()).thenReturn(name);
    return m;
  }
}
