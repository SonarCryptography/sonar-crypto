package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.measures.Metric;
import org.sonarcrypto.analysis.CryptoMetrics;

class CryptoMetricsTest {

  @Test
  void getMetrics() {
    var metrics = new CryptoMetrics().getMetrics();

    assertThat(metrics)
        .contains(
            CryptoMetrics.COMPILE_RUNTIME,
            CryptoMetrics.ANALYSIS_RUNTIME,
            CryptoMetrics.ERRORS_TOTAL,
            CryptoMetrics.CLASSES_ANALYZED,
            CryptoMetrics.METHODS_ANALYZED,
            CryptoMetrics.INPUT_SOURCE)
        .hasSize(14);

    for (var kind : RuleKind.values()) {
      assertThat(metrics)
          .anyMatch(metric -> metric.getKey().equals("crypto.errors." + kind.name().toLowerCase()));
    }
  }

  @Test
  void metricTypes() {
    assertThat(CryptoMetrics.COMPILE_RUNTIME.getType()).isEqualTo(Metric.ValueType.MILLISEC);
    assertThat(CryptoMetrics.ANALYSIS_RUNTIME.getType()).isEqualTo(Metric.ValueType.MILLISEC);
    assertThat(CryptoMetrics.ERRORS_TOTAL.getType()).isEqualTo(Metric.ValueType.INT);
    assertThat(CryptoMetrics.INPUT_SOURCE.getType()).isEqualTo(Metric.ValueType.STRING);
  }

  @Test
  void isEnabled() {
    System.clearProperty("sonarcrypto.measures.enabled");
    assertThat(CryptoMetrics.isEnabled()).isFalse();

    System.setProperty("sonarcrypto.measures.enabled", "true");
    assertThat(CryptoMetrics.isEnabled()).isTrue();

    System.setProperty("sonarcrypto.measures.enabled", "false");
    assertThat(CryptoMetrics.isEnabled()).isFalse();

    System.clearProperty("sonarcrypto.measures.enabled");
  }
}
