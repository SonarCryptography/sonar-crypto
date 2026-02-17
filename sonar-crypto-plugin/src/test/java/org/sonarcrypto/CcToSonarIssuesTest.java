package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import boomerang.scope.ControlFlowGraph;
import boomerang.scope.Statement;
import boomerang.scope.Type;
import boomerang.scope.Val;
import boomerang.scope.WrappedClass;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

  @Test
  void find_input_file_returns_file_for_matching_class() throws IOException {
    InputFile inputFile =
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
    InputFile inputFile =
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

  @Test
  void report_all_issues_creates_issues_for_found_files() throws IOException {
    addJavaFile("com/example/ClassA.java", "package com.example;\npublic class ClassA {}");
    addJavaFile("com/example/ClassB.java", "package com.example;\npublic class ClassB {}");

    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> table = HashBasedTable.create();
    table.put(wrappedClass("com.example.ClassA"), method("doStuff"), Set.of(error()));
    table.put(wrappedClass("com.example.ClassB"), method("encrypt"), Set.of(error()));

    issueReporter.reportAllIssues(sensorContext, table);

    assertThat(sensorContext.allIssues()).hasSize(2);
  }

  @Test
  void report_all_issues_skips_classes_without_source_files() {
    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> table = HashBasedTable.create();
    table.put(wrappedClass("com.example.Missing"), method("m"), Set.of(error()));

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
    table.put(clazz, method("init"), Set.of(error()));
    table.put(clazz, method("encrypt"), Set.of(error()));
    table.put(clazz, method("decrypt"), Set.of(error()));

    issueReporter.reportAllIssues(sensorContext, table);

    assertThat(sensorContext.allIssues()).hasSize(3);
  }

  @Test
  void report_all_issues_includes_method_name_in_message() throws IOException {
    addJavaFile("com/example/Foo.java", "package com.example;\npublic class Foo {}");

    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> table = HashBasedTable.create();
    table.put(wrappedClass("com.example.Foo"), method("encrypt"), Set.of(error()));

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
    return new WrappedClass() {
      @Override
      public String getFullyQualifiedName() {
        return fqn;
      }

      @Override
      public boolean isPhantom() {
        return false;
      }

      @Override
      public boolean isDefined() {
        return true;
      }

      @Override
      public boolean isApplicationClass() {
        return true;
      }

      @Override
      public Collection<boomerang.scope.Method> getMethods() {
        return Collections.emptyList();
      }

      @Override
      public boolean hasSuperclass() {
        return false;
      }

      @Override
      public WrappedClass getSuperclass() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Type getType() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @SuppressWarnings("NullAway")
  private static boomerang.scope.Method method(String name) {
    return new boomerang.scope.Method() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getSubSignature() {
        return "void " + name + "()";
      }

      @Override
      public WrappedClass getDeclaringClass() {
        return wrappedClass("Test");
      }

      @Override
      public boolean isStatic() {
        return false;
      }

      @Override
      public boolean isDefined() {
        return true;
      }

      @Override
      public boolean isPhantom() {
        return false;
      }

      @Override
      public boolean isStaticInitializer() {
        return false;
      }

      @Override
      public boolean isConstructor() {
        return false;
      }

      @Override
      public boolean isParameterLocal(Val v) {
        return false;
      }

      @Override
      public boolean isThisLocal(Val v) {
        return false;
      }

      @Override
      public List<Type> getParameterTypes() {
        return Collections.emptyList();
      }

      @Override
      public Type getParameterType(int i) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Type getReturnType() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Collection<Val> getLocals() {
        return Collections.emptyList();
      }

      @Override
      public Val getThisLocal() {
        throw new UnsupportedOperationException();
      }

      @Override
      public List<Val> getParameterLocals() {
        return Collections.emptyList();
      }

      @Override
      public List<Statement> getStatements() {
        return Collections.emptyList();
      }

      @Override
      public ControlFlowGraph getControlFlowGraph() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @SuppressWarnings("NullAway")
  private static AbstractError error() {
    return new AbstractError(null, null, null) {
      @Override
      public String toErrorMarkerString() {
        return "test error";
      }
    };
  }
}
