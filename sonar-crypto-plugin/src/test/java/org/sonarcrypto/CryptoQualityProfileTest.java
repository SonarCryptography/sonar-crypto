package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;

class CryptoQualityProfileTest {

  @Test
  void define() {
    BuiltInQualityProfilesDefinition.Context context =
        new BuiltInQualityProfilesDefinition.Context();

    new CryptoQualityProfile().define(context);

    BuiltInQualityProfile profile = context.profile("java", "Crypto Security");
    assertThat(profile).isNotNull();
    assertThat(profile.language()).isEqualTo("java");
    assertThat(profile.isDefault()).isTrue();
    assertThat(profile.rules()).hasSize(1);
    assertThat(profile.rule(CryptoRulesDefinition.CC_RULE)).isNotNull();
  }
}
