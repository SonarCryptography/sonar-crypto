package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

class CryptoSensorTest {
  @RegisterExtension LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @TempDir Path tempDir;

  @Test
  void describe_sets_sensor_name() {
    CryptoSensor sensor = new CryptoSensor();
    SensorDescriptor descriptor = mock(SensorDescriptor.class);
    when(descriptor.name("CogniCryptSensor")).thenReturn(descriptor);

    sensor.describe(descriptor);

    verify(descriptor).name("CogniCryptSensor");
  }

  @Test
  void describe_registers_java_language() {
    CryptoSensor sensor = new CryptoSensor();
    SensorDescriptor descriptor = mock(SensorDescriptor.class);
    when(descriptor.name("CogniCryptSensor")).thenReturn(descriptor);

    sensor.describe(descriptor);

    verify(descriptor).onlyOnLanguages("java");
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
    fakeErrors.put(
        wrappedClass("com.example.Foo"), method("encrypt"), Set.of(mock(AbstractError.class)));
    fakeErrors.put(
        wrappedClass("com.example.Bar"), method("decrypt"), Set.of(mock(AbstractError.class)));
    fakeErrors.put(
        wrappedClass("com.example.Foo"), method("hash"), Set.of(mock(AbstractError.class)));

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
