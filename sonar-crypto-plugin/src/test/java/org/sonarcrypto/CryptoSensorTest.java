package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
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

    assertThat(logTester.logs()).containsExactly("Failed to build Maven project");
  }

  @Test
  void execute_creates_no_issues_for_non_maven_project() {
    CryptoSensor sensor = new CryptoSensor();
    SensorContextTester context = SensorContextTester.create(tempDir);

    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
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
