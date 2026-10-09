package org.sonarcrypto.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonarcrypto.RuleKind;

class CryptoAnalysisInfoTest {

  @TempDir Path tempDir;

  @Test
  void writeJson() throws IOException {
    var ruleKind = RuleKind.values()[0];
    var errorsPerRuleKind = new EnumMap<RuleKind, Integer>(RuleKind.class);
    errorsPerRuleKind.put(ruleKind, 3);
    var info =
        new CryptoAnalysisInfo()
            .put(MetricDefinitions.INPUT_SOURCE, InputSource.MAVEN)
            .put(MetricDefinitions.COMPILE_MILLIS, 42L)
            .put(MetricDefinitions.CLASSPATH_FALLBACK, true)
            .put(MetricDefinitions.ERRORS_PER_RULE_KIND, errorsPerRuleKind)
            .put(MetricDefinitions.ERRORS_PER_CRYSL_RULE, Map.of("b.Rule", 1, "a.Rule", 2));
    var file = tempDir.resolve("nested/metrics.json");

    info.writeJson(file);

    Map<String, Object> json =
        new Gson()
            .fromJson(Files.readString(file), new TypeToken<Map<String, Object>>() {}.getType());
    assertThat(json).containsOnlyKeys("schemaVersion", "timestamp", "metrics");
    // Gson reads all JSON numbers back as doubles.
    assertThat(json.get("schemaVersion")).isEqualTo((double) CryptoAnalysisInfo.SCHEMA_VERSION);
    assertThat(Instant.parse((String) json.get("timestamp"))).isBeforeOrEqualTo(Instant.now());
    // Rule kinds without errors are written as 0.
    var expectedPerRuleKind = new LinkedHashMap<String, Object>();
    for (var kind : RuleKind.values()) {
      expectedPerRuleKind.put(kind.name(), kind == ruleKind ? 3.0 : 0.0);
    }
    assertThat(json.get("metrics"))
        .isEqualTo(
            Map.of(
                "inputSource",
                "MAVEN",
                "compileMillis",
                42.0,
                "classpathFallback",
                true,
                "errorsPerRuleKind",
                expectedPerRuleKind,
                "errorsPerCryslRule",
                Map.of("a.Rule", 2.0, "b.Rule", 1.0)));
    // Free-form counts are written sorted by key.
    assertThat(Files.readString(file)).containsSubsequence("\"a.Rule\"", "\"b.Rule\"");
  }

  @Test
  void time() {
    var info = new CryptoAnalysisInfo();

    assertThat(info.timeAndGet(MetricDefinitions.COMPILE_MILLIS, () -> "result"))
        .isEqualTo("result");
    assertThatThrownBy(
            () ->
                info.time(
                    MetricDefinitions.ANALYSIS_MILLIS,
                    () -> {
                      throw new IllegalStateException("boom");
                    }))
        .hasMessage("boom");

    assertThat(info.get(MetricDefinitions.COMPILE_MILLIS)).isNotNegative();
    // Failing actions are timed too.
    assertThat(info.get(MetricDefinitions.ANALYSIS_MILLIS)).isNotNegative();
  }
}
