package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import boomerang.scope.WrappedClass;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarcrypto.ccerror.ConvertedError;
import org.sonarcrypto.ccerror.causes.UndefinedCause;
import org.sonarcrypto.ccerror.violations.CallViolation;

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

  @Test
  void report_all_issues_creates_issues_for_found_files() throws IOException {
    final var inputFile1 =
        addJavaFile("com/example/ClassA.java", "package com.example;\npublic class ClassA {}");
    final var inputFile2 =
        addJavaFile("com/example/ClassB.java", "package com.example;\npublic class ClassB {}");

    final var errors =
        List.of(
            new ConvertedError(
                inputFile1,
                new DefaultTextRange(new DefaultTextPointer(1, 21), new DefaultTextPointer(1, 42)),
                method("encrypt"),
                new CallViolation(RuleKind.GENERAL, new UndefinedCause("Undefined"))),
            new ConvertedError(
                inputFile1,
                new DefaultTextRange(
                    new DefaultTextPointer(10, 39), new DefaultTextPointer(10, 56)),
                method("decrypt"),
                new CallViolation(RuleKind.GENERAL, new UndefinedCause("Undefined"))),
            new ConvertedError(
                inputFile2,
                new DefaultTextRange(
                    new DefaultTextPointer(21, 17), new DefaultTextPointer(21, 23)),
                method("init"),
                new CallViolation(RuleKind.GENERAL, new UndefinedCause("Undefined"))));

    issueReporter.reportAllIssues(sensorContext, errors);

    assertThat(sensorContext.allIssues()).hasSize(2);
  }

  @Test
  void report_all_issues_reports_errors_from_multiple_methods() throws IOException {
    final var inputFile =
        addJavaFile("com/example/Buggy.java", "package com.example;\npublic class Buggy {}");

    final var errors =
        List.of(
            new ConvertedError(
                inputFile,
                new DefaultTextRange(new DefaultTextPointer(1, 21), new DefaultTextPointer(1, 42)),
                method("encrypt"),
                new CallViolation(RuleKind.GENERAL, new UndefinedCause("Undefined"))),
            new ConvertedError(
                inputFile,
                new DefaultTextRange(
                    new DefaultTextPointer(10, 39), new DefaultTextPointer(10, 56)),
                method("decrypt"),
                new CallViolation(RuleKind.GENERAL, new UndefinedCause("Undefined"))),
            new ConvertedError(
                inputFile,
                new DefaultTextRange(
                    new DefaultTextPointer(21, 17), new DefaultTextPointer(21, 23)),
                method("init"),
                new CallViolation(RuleKind.GENERAL, new UndefinedCause("Undefined"))));

    issueReporter.reportAllIssues(sensorContext, errors);

    assertThat(sensorContext.allIssues()).hasSize(3);
  }

  @Test
  void report_all_issues_includes_method_name_in_message() throws IOException {
    final var inputFile =
        addJavaFile("com/example/Foo.java", "package com.example;\npublic class Foo {}");
    final var error =
        new ConvertedError(
            inputFile,
            new DefaultTextRange(new DefaultTextPointer(1, 21), new DefaultTextPointer(1, 42)),
            method("encrypt"),
            new CallViolation(RuleKind.GENERAL, new UndefinedCause("Undefined")));

    issueReporter.reportAllIssues(sensorContext, List.of(error));

    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.primaryLocation().message()).contains("encrypt");
  }

  @Test
  void report_all_issues_handles_empty_table() {
    issueReporter.reportAllIssues(sensorContext, List.of(/* empty */ ));

    assertThat(sensorContext.allIssues()).isEmpty();
  }

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
    assertThat(issue.ruleKey())
        .hasToString(CryptoRulesDefinitions.ALGORITHM.getRuleKey().toString());
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

    String customMessage = "Weak encryption actualAlgorithm used";
    issueReporter.reportIssue(sensorContext, inputFile, 1, customMessage);

    Issue issue = sensorContext.allIssues().iterator().next();
    assertThat(issue.primaryLocation().message())
        .startsWith("Cryptographic API misuse:")
        .contains(customMessage);
  }

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
    var declaringClassMock = wrappedClass("com.example");
    boomerang.scope.Method methodMock = mock(boomerang.scope.Method.class);
    when(methodMock.getName()).thenReturn(name);
    when(methodMock.getDeclaringClass()).thenReturn(declaringClassMock);
    return methodMock;
  }
}
