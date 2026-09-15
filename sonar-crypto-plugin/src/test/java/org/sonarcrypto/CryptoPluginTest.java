package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;
import org.sonarcrypto.analysis.CryptoMetrics;

class CryptoPluginTest {

  @Test
  void define() {
    System.clearProperty("sonarcrypto.measures.enabled");
    SonarRuntime runtime = mock(SonarRuntime.class);
    when(runtime.getApiVersion()).thenReturn(Version.create(13, 4));
    when(runtime.getProduct()).thenReturn(SonarProduct.SONARQUBE);
    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);
    when(runtime.getEdition()).thenReturn(SonarEdition.COMMUNITY);
    Plugin.Context context = new Plugin.Context(runtime);

    new CryptoPlugin().define(context);

    assertThat(context.getExtensions())
        .contains(CryptoRulesDefinitions.ALGORITHM, CryptoQualityProfile.class, CryptoSensor.class)
        .doesNotContain(CryptoMetrics.class);
  }

  @Test
  void define_registers_metrics_when_enabled() {
    System.setProperty("sonarcrypto.measures.enabled", "true");
    try {
      SonarRuntime runtime = mock(SonarRuntime.class);
      when(runtime.getApiVersion()).thenReturn(Version.create(13, 4));
      when(runtime.getProduct()).thenReturn(SonarProduct.SONARQUBE);
      when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);
      when(runtime.getEdition()).thenReturn(SonarEdition.COMMUNITY);
      Plugin.Context context = new Plugin.Context(runtime);

      new CryptoPlugin().define(context);

      assertThat(context.getExtensions()).contains(CryptoMetrics.class);
    } finally {
      System.clearProperty("sonarcrypto.measures.enabled");
    }
  }
}
