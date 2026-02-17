package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

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
    SonarRuntime runtime =
        new SonarRuntime() {
          @Override
          public Version getApiVersion() {
            return Version.create(13, 4);
          }

          @Override
          public SonarProduct getProduct() {
            return SonarProduct.SONARQUBE;
          }

          @Override
          public SonarQubeSide getSonarQubeSide() {
            return SonarQubeSide.SCANNER;
          }

          @Override
          public SonarEdition getEdition() {
            return SonarEdition.COMMUNITY;
          }
        };
    return new Plugin.Context(runtime);
  }
}
