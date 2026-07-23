package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonarcrypto.cryptorules.CryptoRulesDefinition.REPOSITORY_KEY;
import static org.sonarcrypto.utils.sonar.SonarFileSystemUtils.findInputFile;
import static org.sonarcrypto.utils.sonar.TextUtils.quote;
import static org.sonarcrypto.utils.test.sonarcontext.SonarContextTesterUtils.initializeFileSystem;

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.utility.groundtruth.GroundTruthParser;
import org.sonarcrypto.utility.groundtruth.GroundTruthUtils;
import org.sonarcrypto.utility.groundtruth.ValueSupport;
import org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils;
import org.sonarcrypto.utils.maven.MavenBuildException;

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
    context.fileSystem().setWorkDir(tempDir);

    sensor.execute(context);

    assertThat(context.allIssues()).isEmpty();
    assertThat(logTester.logs()).contains("Failed to build Maven project");
  }

  @Test
  void testExecuteMavenProject() throws IOException, MavenBuildException {
    CryptoSensor sensor = new CryptoSensor();
    SensorContextTester context =
        SensorContextTester.create(Path.of("../e2e/src/test/resources/Java/Maven/Basic"));
    initializeFileSystem(context);

    final var foundErrors = sensor.scan(context.fileSystem(), sensor.extractRules());
    sensor.report(context, foundErrors);

    final var groundTruth = new GroundTruthParser().parse(context.fileSystem());

    final var combinedMap = new TreeMap<GroundTruthParser.Location, Entry>();

    groundTruth.forEach(
        (location, gts) -> {
          final var entry = combinedMap.computeIfAbsent(location, ignored -> new Entry());

          gts.forEach(
              it -> entry.expected.add(new Item(it.ruleKind(), it.causeType(), it.value())));
        });

    foundErrors.forEach(
        error -> {
          // Find the InputFile corresponding to this class
          InputFile inputFile = findInputFile(context.fileSystem(), error.className());

          assertThat(inputFile)
              .withFailMessage("Input file for class not found!\nError: %s")
              .isNotNull();

          final var position = ConverterUtils.selectLocation(inputFile, error.position());

          final var entry =
              combinedMap.computeIfAbsent(
                  new GroundTruthParser.Location(inputFile.filename(), position.start().line()),
                  _location -> new Entry());
          final var violation = error.violation();
          final var item =
              new Item(
                  violation.getRulesDefinition().getRuleKind(),
                  violation.getCause().getClass(),
                  ValueSupport.getValue(violation.getCause()));
          entry.actual.add(item);
          entry.count++;
        });

    var invalidResult = false;

    for (var entry : combinedMap.values()) {
      final var actual = entry.actual;
      final var actualCopy = new HashSet<>(actual);
      final var expected = entry.expected;
      actual.removeAll(expected);
      expected.removeAll(actualCopy);
    }

    for (var combined : combinedMap.entrySet()) {
      final var location = combined.getKey();
      final var entry = combined.getValue();
      final var actual = entry.actual;
      final var expected = entry.expected;

      if (!actual.isEmpty() || !expected.isEmpty()) {
        invalidResult = true;
        System.out.println();
        System.out.println(location);
      }

      if (!actual.isEmpty()) {
        System.out.println("    False Positives: ");
        actual.forEach(it -> System.out.println("        " + it));
      }

      if (!expected.isEmpty()) {
        if (!actual.isEmpty()) {
          System.out.println();
        }
        System.out.println("    False negatives!");
        expected.forEach(it -> System.out.println("        " + it));
      }
    }

    if (invalidResult) {
      fail("Invalid result!");
    }

    final var sonarIssueCount =
        context.allIssues().stream()
            .map(it -> it.ruleKey().repository())
            .filter(REPOSITORY_KEY::equals)
            .count();

    final var processedCount = combinedMap.values().stream().mapToLong(it -> it.count).sum();

    assertThat(processedCount)
        .withFailMessage(
            "Invalid number of issues reported!\nActual: %d\nExpected: %d",
            processedCount, sonarIssueCount)
        .isEqualTo(sonarIssueCount);
  }

  @Test
  void scan_prefers_jimple_input_when_bridge_output_exists() throws IOException {
    CryptoSensor sensor = new CryptoSensor();
    SensorContextTester context = SensorContextTester.create(tempDir);
    context.fileSystem().setWorkDir(tempDir);

    final var jimpleDir = tempDir.resolve("bridge-output/jimple");
    final var filesystem = context.fileSystem();
    final var rules = sensor.extractRules();
    Files.createDirectories(jimpleDir);
    Files.writeString(jimpleDir.resolve("Invalid.jimple"), "invalid jimple");
    assertThatThrownBy(() -> sensor.scan(filesystem, rules))
        .isInstanceOfAny(AssertionError.class, RuntimeException.class);
    assertThat(logTester.logs())
        .anyMatch(it -> it.contains("Using Jimple files from bridge output"))
        .anyMatch(it -> it.contains("Falling back to ruleset dependencies only."));
  }

  @Test
  void resolveAnalysisClassPath() throws Exception {
    final var result =
        (String)
            invokePrivateStatic(
                "resolveAnalysisClassPath",
                new Class<?>[] {String.class, String.class},
                Path.of("../e2e/src/test/resources/Java/Maven/Basic")
                    .toAbsolutePath()
                    .normalize()
                    .toString(),
                "rules.jar");

    assertThat(result)
        .startsWith("rules.jar" + java.io.File.pathSeparator)
        .contains("bcprov-jdk18on");
  }

  private static Object invokePrivateStatic(
      String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
    Method method = CryptoSensor.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    try {
      return method.invoke(null, args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception cause) {
        throw cause;
      }
      throw e;
    }
  }

  private static final class Entry {
    public final Set<Item> actual = new HashSet<>();
    public final Set<Item> expected = new HashSet<>();
    public int count;
  }

  public record Item(RuleKind ruleKind, Class<? extends Cause> causeType, @Nullable String value) {
    @Override
    public String toString() {
      final var sb =
          new StringBuilder()
              .append(ruleKind)
              .append('/')
              .append(GroundTruthUtils.toShortString(causeType));

      if (value != null) {
        sb.append(' ').append(quote(value));
      }

      return sb.toString();
    }
  }
}
