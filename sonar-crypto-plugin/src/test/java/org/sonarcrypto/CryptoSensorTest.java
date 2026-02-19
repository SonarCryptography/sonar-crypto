package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonarcrypto.utils.test.asserts.CcErrorsAssert.assertOnFilteredCcErrorsThat;

import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.TypestateError;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;
import org.sonarcrypto.utils.test.runner.MavenProjectTestRunner;

@NullMarked
class CryptoSensorTest {
  @RegisterExtension LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @TempDir Path tempDir;

  @Test
  void describe() {
    CryptoSensor sensor = new CryptoSensor();
    SensorDescriptor descriptor = mock(SensorDescriptor.class);
    when(descriptor.name("CogniCryptSensor")).thenReturn(descriptor);

    sensor.describe(descriptor);

    verify(descriptor).name("CogniCryptSensor");
    verify(descriptor).onlyOnLanguages("java");
  }

  @Test
  void execute_fails_for_non_maven_project() {
    CryptoSensor sensor = new CryptoSensor();
    SensorContextTester context = SensorContextTester.create(tempDir);

    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).containsExactly("Failed to build Maven project");
  }

  @Test
  void mavenProjectTest() throws IOException, URISyntaxException {
    final var runner = new MavenProjectTestRunner();

    final var collectedErrors =
        runner.run("../e2e/src/test/resources/Java/Maven/Basic", Ruleset.JCA).collectedErrors();

    assertOnFilteredCcErrorsThat(
            collectedErrors,
            "com.example.crypto.WeakCryptoExamples",
            "byte[] encryptWithDES(byte[])")
        .anySatisfy(
            error ->
                assertThat(error).isInstanceOfAny(ConstraintError.class, TypestateError.class));
  }
}
