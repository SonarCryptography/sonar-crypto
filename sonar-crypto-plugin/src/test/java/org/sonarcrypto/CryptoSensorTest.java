package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonarcrypto.cryptorules.CryptoRulesDefinition.REPOSITORY_KEY;
import static org.sonarcrypto.utils.sonar.TextUtils.quote;
import static org.sonarcrypto.utils.test.sonarcontext.SonarContextTesterUtils.initializeFileSystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.utility.groundtruth.GroundTruthParser;
import org.sonarcrypto.utility.groundtruth.GroundTruthUtils;
import org.sonarcrypto.utility.groundtruth.ValueSupport;

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
  void testExecuteMavenProject() throws IOException {
    CryptoSensor sensor = new CryptoSensor();
    SensorContextTester context =
        SensorContextTester.create(Path.of("../e2e/src/test/resources/Java/Maven/Basic"));
    initializeFileSystem(context);

    final var foundErrors = sensor.scan(context.fileSystem(), sensor.extractRules());
    sensor.report(context, foundErrors);

    final var groundTruth = new GroundTruthParser().parse(context.fileSystem());
    // groundTruth.forEach((key, value) -> System.out.println(key + " -> " + value));

    final var combinedMap = new TreeMap<GroundTruthParser.Location, Entry>();

    final var expectedErrorCount =
        new Object() {
          long min = 0;
          long max = 0;
        };

    groundTruth.forEach(
        (location, gts) -> {
          final var entry =
              combinedMap.computeIfAbsent(
                  location, _location -> new Entry(new HashSet<>(), new HashMap<>()));
          gts.forEach(
              it -> {
                expectedErrorCount.max++;

                if (!it.isOptional()) {
                  expectedErrorCount.min++;
                }

                entry.expected.put(
                    new Item(it.ruleKind(), it.causeType(), it.value()), it.isOptional());
              });
        });

    foundErrors.forEach(
        error -> {
          final var entry =
              combinedMap.computeIfAbsent(
                  new GroundTruthParser.Location(
                      error.inputFile().filename(), error.position().start().line()),
                  location1 -> new Entry(new HashSet<>(), new HashMap<>()));
          final var violation = error.violation();
          final var item =
              new Item(
                  violation.getRulesDefinition().getRuleKind(),
                  violation.getCause().getClass(),
                  ValueSupport.getValue(violation.getCause()));
          entry.actual.add(item);
        });

    var invalidResult = false;

    for (var entry : combinedMap.values()) {
      final var actual = entry.actual();
      final var actualCopy = new HashSet<>(actual);
      final var expected = entry.expected().keySet();
      actual.removeAll(expected);
      expected.removeAll(actualCopy);
    }

    for (var entry : combinedMap.values()) {
      entry.expected().entrySet().removeIf(Map.Entry::getValue);
    }

    for (var combined : combinedMap.entrySet()) {
      final var location = combined.getKey();
      final var entry = combined.getValue();
      final var actual = entry.actual();
      final var expected = entry.expected().keySet();

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

    final var actualCount =
        context.allIssues().stream()
            .map(it -> it.ruleKey().repository())
            .filter(REPOSITORY_KEY::equals)
            .count();

    assertThat(actualCount)
        .withFailMessage(
            "Wrong number of issues reported!\nActual: %d\nExpected: %d..%d",
            actualCount, expectedErrorCount.min, expectedErrorCount.max)
        .isBetween(expectedErrorCount.min, expectedErrorCount.max);
  }

  private record Entry(Set<Item> actual, Map<Item, Boolean> expected) {}

  public record Item(
      RuleKind ruleKind,
      Class<? extends Cause> causeType,
      @Nullable String value,
      boolean isOptional) {
    public Item(RuleKind ruleKind, Class<? extends Cause> causeType, @Nullable String value) {
      this(ruleKind, causeType, value, false);
    }

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
