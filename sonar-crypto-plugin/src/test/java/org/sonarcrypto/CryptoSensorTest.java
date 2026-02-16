package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.TypestateError;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarcrypto.runner.CollectedErrorsAsserter;
import org.sonarcrypto.runner.MavenProjectTestRunner;

@NullMarked
class CryptoSensorTest {
  @RegisterExtension LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @TempDir Path tempDir;

  @Test
  void fails_to_build_for_non_maven_project_dir() {
    CryptoSensor sensor = new CryptoSensor();
    sensor.execute(SensorContextTester.create(tempDir));
    assertThat(logTester.logs()).containsExactly("Failed to build Maven project");
  }

  @Test
  void mavenProjectTest() throws IOException, URISyntaxException {
    final var runner = new MavenProjectTestRunner();

    final var collectedErrors =
        runner.run("../e2e/src/test/resources/Java/Maven/Basic", Ruleset.JCA);

    new CollectedErrorsAsserter(collectedErrors)
        .assertContainsAny(
            "com.example.crypto.WeakCryptoExamples",
            "byte[] encryptWithDES(byte[])",
            List.of(ConstraintError.class, TypestateError.class));
  }
}
