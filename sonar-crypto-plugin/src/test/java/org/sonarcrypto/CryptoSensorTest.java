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
import java.util.Map;
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
import org.sonarcrypto.analysis.CryptoAnalysisInfo;
import org.sonarcrypto.analysis.InputSource;
import org.sonarcrypto.analysis.MetricDefinitions;
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
    // Metrics are written for failed runs too.
    assertThat(tempDir.resolve(CryptoSensor.DEFAULT_METRICS_FILE))
        .content()
        .contains("\"outcome\": \"COMPILE_FAILED\"")
        .contains("\"ruleset\": \"JCA_BC_JCA\"")
        .contains("\"ruleExtractionMillis\"")
        .contains("\"totalMillis\"")
        .doesNotContain("\"analysisMillis\"");
  }

  @Test
  void testExecuteMavenProject() throws IOException, MavenBuildException {
    CryptoSensor sensor = new CryptoSensor();
    SensorContextTester context =
        SensorContextTester.create(Path.of("../e2e/src/test/resources/Java/Maven/Basic"));
    initializeFileSystem(context);
    context.fileSystem().setWorkDir(tempDir);

    final var info = new CryptoAnalysisInfo();
    final var foundErrors = sensor.scan(context.fileSystem(), sensor.extractRules(), info);
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

    assertThat(info.get(MetricDefinitions.INPUT_SOURCE)).isEqualTo(InputSource.MAVEN);
    assertThat(info.get(MetricDefinitions.COMPILE_MILLIS)).isNotNegative();
    assertThat(info.get(MetricDefinitions.CLASSPATH_FALLBACK)).isFalse();
    assertThat(info.get(MetricDefinitions.ANALYSIS_MILLIS)).isPositive();
    assertThat(info.get(MetricDefinitions.TOTAL_ERRORS)).isEqualTo(foundErrors.size());
    assertThat(info.get(MetricDefinitions.ERRORS_PER_RULE_KIND)).isNotEmpty();
    assertThat(info.get(MetricDefinitions.ERRORS_PER_CRYSL_RULE))
        .isNotEmpty()
        .allSatisfy((rule, count) -> assertThat(rule).contains("."))
        .satisfies(
            counts ->
                assertThat(counts.values().stream().mapToInt(Integer::intValue).sum())
                    .isEqualTo(foundErrors.size()));
    assertThat(info.get(MetricDefinitions.SEEDS))
        .isGreaterThanOrEqualTo(info.get(MetricDefinitions.METHODS_WITH_CRYPTO_USAGE));
    assertThat(info.get(MetricDefinitions.CLASSES_WITH_CRYPTO_USAGE)).isPositive();
    assertThat(info.get(MetricDefinitions.METHODS_WITH_CRYPTO_USAGE)).isPositive();
    assertThat(info.asMap())
        .containsOnlyKeys(
            MetricDefinitions.INPUT_SOURCE.name(),
            MetricDefinitions.COMPILE_MILLIS.name(),
            MetricDefinitions.CLASSPATH_FALLBACK.name(),
            MetricDefinitions.ANALYSIS_MILLIS.name(),
            MetricDefinitions.TOTAL_ERRORS.name(),
            MetricDefinitions.ERRORS_PER_RULE_KIND.name(),
            MetricDefinitions.ERRORS_PER_CRYSL_RULE.name(),
            MetricDefinitions.SEEDS.name(),
            MetricDefinitions.CLASSES_WITH_CRYPTO_USAGE.name(),
            MetricDefinitions.METHODS_WITH_CRYPTO_USAGE.name())
        .containsEntry(MetricDefinitions.INPUT_SOURCE.name(), InputSource.MAVEN.name())
        .hasEntrySatisfying(
            MetricDefinitions.ERRORS_PER_RULE_KIND.name(),
            counts ->
                assertThat((Map<?, ?>) counts)
                    .hasSize(RuleKind.values().length)
                    .allSatisfy((kind, count) -> assertThat(kind).isInstanceOf(String.class)));
  }

  @Test
  void writeMetrics() throws IOException {
    SensorContextTester context = SensorContextTester.create(tempDir);
    context.fileSystem().setWorkDir(tempDir.resolve("work"));
    final var info = new CryptoAnalysisInfo().put(MetricDefinitions.TOTAL_ERRORS, 0);

    context.config().setProperty(CryptoSensor.METRICS_FILE_PROPERTY, "out/metrics.json");
    CryptoSensor.writeMetrics(context, info);
    assertThat(tempDir.resolve("out/metrics.json")).content().contains("\"totalErrors\": 0");
    assertThat(tempDir.resolve("work").resolve(CryptoSensor.DEFAULT_METRICS_FILE)).doesNotExist();

    // A file cannot be used as parent directory, so writing fails with a warning only.
    Files.writeString(tempDir.resolve("blocked"), "");
    context.config().setProperty(CryptoSensor.METRICS_FILE_PROPERTY, "blocked/metrics.json");
    CryptoSensor.writeMetrics(context, info);
    assertThat(logTester.logs())
        .anyMatch(it -> it.contains("Failed to write crypto analysis metrics"));
  }

  @Test
  void scan_prefers_jimple_input_when_bridge_output_exists() throws IOException {
    CryptoSensor sensor = new CryptoSensor();
    SensorContextTester context = SensorContextTester.create(tempDir);
    context.fileSystem().setWorkDir(tempDir);

    final var jimpleDir = tempDir.resolve("bridge-output/jimple");
    final var filesystem = context.fileSystem();
    final var rules = sensor.extractRules();
    final var info = new CryptoAnalysisInfo();
    Files.createDirectories(jimpleDir);
    Files.writeString(jimpleDir.resolve("Invalid.jimple"), "invalid jimple");
    assertThatThrownBy(() -> sensor.scan(filesystem, rules, info))
        .isInstanceOfAny(AssertionError.class, RuntimeException.class);
    assertThat(info.get(MetricDefinitions.INPUT_SOURCE)).isEqualTo(InputSource.JIMPLE);
    assertThat(info.get(MetricDefinitions.CLASSPATH_FALLBACK)).isTrue();
    // The failed analysis is still timed.
    assertThat(info.get(MetricDefinitions.ANALYSIS_MILLIS)).isNotNegative();
    assertThat(logTester.logs())
        .anyMatch(it -> it.contains("Using Jimple files from bridge output"))
        .anyMatch(it -> it.contains("Falling back to ruleset dependencies only."));
  }

  @Test
  void resolveMavenClassPath() throws Exception {
    final var result =
        (String)
            invokePrivateStatic(
                "resolveMavenClassPath",
                new Class<?>[] {String.class},
                Path.of("../e2e/src/test/resources/Java/Maven/Basic")
                    .toAbsolutePath()
                    .normalize()
                    .toString());

    assertThat(result).contains("bcprov-jdk18on");
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
