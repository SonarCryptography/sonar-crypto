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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

class CryptoSensorTest {
  @RegisterExtension LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @TempDir Path tempDir;

  @Test
  void describe_sets_sensor_name() {
    CryptoSensor sensor = new CryptoSensor();
    CapturingSensorDescriptor descriptor = new CapturingSensorDescriptor();

    sensor.describe(descriptor);

    assertThat(descriptor.name).isEqualTo("CogniCryptSensor");
  }

  @Test
  void describe_registers_java_language() {
    CryptoSensor sensor = new CryptoSensor();
    CapturingSensorDescriptor descriptor = new CapturingSensorDescriptor();

    sensor.describe(descriptor);

    assertThat(descriptor.languages).containsExactly("java");
  }

  @Test
  void execute_logs_error_for_non_maven_project() {
    CryptoSensor sensor = new CryptoSensor();
    sensor.execute(SensorContextTester.create(tempDir));

    assertThat(logTester.logs()).containsExactly("Cryptographic analysis failed");
  }

  @Test
  void execute_creates_no_issues_for_non_maven_project() {
    CryptoSensor sensor = new CryptoSensor();
    SensorContextTester context = SensorContextTester.create(tempDir);

    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void execute_logs_error_count_and_reports_issues() throws IOException {
    Table<WrappedClass, boomerang.scope.Method, Set<AbstractError>> fakeErrors =
        HashBasedTable.create();
    fakeErrors.put(wrappedClass("com.example.Foo"), method("encrypt"), Set.of(error()));
    fakeErrors.put(wrappedClass("com.example.Bar"), method("decrypt"), Set.of(error()));
    fakeErrors.put(wrappedClass("com.example.Foo"), method("hash"), Set.of(error()));

    CryptoSensor sensor = new CryptoSensor(new CcToSonarIssues(), projectPath -> fakeErrors);

    SensorContextTester context = SensorContextTester.create(tempDir);
    addJavaFile(context, "com/example/Foo.java", "package com.example;\npublic class Foo {}");
    addJavaFile(context, "com/example/Bar.java", "package com.example;\npublic class Bar {}");

    sensor.execute(context);

    assertThat(logTester.logs()).anyMatch(log -> log.contains("Found 3 cryptographic errors"));
    assertThat(context.allIssues()).hasSize(3);
  }

  private void addJavaFile(SensorContextTester context, String relativePath, String content)
      throws IOException {
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
    context.fileSystem().add(inputFile);
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

  /** Simple capturing implementation of SensorDescriptor for testing. */
  private static class CapturingSensorDescriptor implements SensorDescriptor {
    String name;
    final List<String> languages = new ArrayList<>();

    @Override
    public SensorDescriptor name(String sensorName) {
      this.name = sensorName;
      return this;
    }

    @Override
    public SensorDescriptor onlyOnLanguage(String languageKey) {
      languages.add(languageKey);
      return this;
    }

    @Override
    public SensorDescriptor onlyOnLanguages(String... languageKeys) {
      languages.addAll(List.of(languageKeys));
      return this;
    }

    @Override
    public SensorDescriptor onlyOnFileType(InputFile.Type type) {
      return this;
    }

    @Override
    public SensorDescriptor createIssuesForRuleRepository(String... repositoryKey) {
      return this;
    }

    @Override
    public SensorDescriptor createIssuesForRuleRepositories(String... repositoryKeys) {
      return this;
    }

    @Override
    public SensorDescriptor global() {
      return this;
    }

    @Override
    public SensorDescriptor onlyWhenConfiguration(Predicate<Configuration> predicate) {
      return this;
    }

    @Override
    public SensorDescriptor processesFilesIndependently() {
      return this;
    }

    @Override
    public SensorDescriptor processesHiddenFiles() {
      return this;
    }
  }
}
