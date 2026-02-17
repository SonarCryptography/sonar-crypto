package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;

class CryptoQualityProfileTest {

  @Test
  void define_creates_profile_for_java() {
    BuiltInQualityProfilesDefinition.Context context =
        new BuiltInQualityProfilesDefinition.Context();
    new CryptoQualityProfile().define(context);

    BuiltInQualityProfile profile = context.profile("java", "Crypto Security");
    assertThat(profile).isNotNull();
    assertThat(profile.language()).isEqualTo("java");
  }

  @Test
  void define_sets_profile_as_default() {
    BuiltInQualityProfilesDefinition.Context context =
        new BuiltInQualityProfilesDefinition.Context();
    new CryptoQualityProfile().define(context);

    BuiltInQualityProfile profile = context.profile("java", "Crypto Security");
    assertThat(profile.isDefault()).isTrue();
  }

  @Test
  void define_activates_cc1_rule() {
    BuiltInQualityProfilesDefinition.Context context =
        new BuiltInQualityProfilesDefinition.Context();
    new CryptoQualityProfile().define(context);

    BuiltInQualityProfile profile = context.profile("java", "Crypto Security");
    assertThat(profile.rules()).hasSize(1);
    assertThat(profile.rule(CryptoRulesDefinition.CC_RULE)).isNotNull();
  }
}
