package org.sonarcrypto.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
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
            .put(MetricDefinitions.ERRORS_PER_RULE_KIND, errorsPerRuleKind);
    var file = tempDir.resolve("nested/metrics.json");

    info.writeJson(file);

    Map<String, Object> json =
        new Gson()
            .fromJson(Files.readString(file), new TypeToken<Map<String, Object>>() {}.getType());
    assertThat(json).containsOnlyKeys("schemaVersion", "timestamp", "metrics");
    // Gson reads all JSON numbers back as doubles.
    assertThat(json.get("schemaVersion")).isEqualTo((double) CryptoAnalysisInfo.SCHEMA_VERSION);
    assertThat(Instant.parse((String) json.get("timestamp"))).isBeforeOrEqualTo(Instant.now());
    assertThat(json.get("metrics"))
        .isEqualTo(
            Map.of(
                "inputSource",
                "MAVEN",
                "compileMillis",
                42.0,
                "errorsPerRuleKind",
                Map.of(ruleKind.name(), 3.0)));
  }
}
