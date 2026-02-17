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

class CryptoPluginTest {

  @Test
  void define_registers_rules_definition() {
    Plugin.Context context = newContext();
    new CryptoPlugin().define(context);

    assertThat(context.getExtensions()).contains(CryptoRulesDefinition.class);
  }

  @Test
  void define_registers_quality_profile() {
    Plugin.Context context = newContext();
    new CryptoPlugin().define(context);

    assertThat(context.getExtensions()).contains(CryptoQualityProfile.class);
  }

  @Test
  void define_registers_sensor() {
    Plugin.Context context = newContext();
    new CryptoPlugin().define(context);

    assertThat(context.getExtensions()).contains(CryptoSensor.class);
  }

  @Test
  void define_registers_exactly_three_extensions() {
    Plugin.Context context = newContext();
    new CryptoPlugin().define(context);

    assertThat(context.getExtensions()).hasSize(3);
  }

  private static Plugin.Context newContext() {
    SonarRuntime runtime = mock(SonarRuntime.class);
    when(runtime.getApiVersion()).thenReturn(Version.create(13, 4));
    when(runtime.getProduct()).thenReturn(SonarProduct.SONARQUBE);
    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SCANNER);
    when(runtime.getEdition()).thenReturn(SonarEdition.COMMUNITY);
    return new Plugin.Context(runtime);
  }
}
