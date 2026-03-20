package org.sonarcrypto;

import static org.sonarcrypto.cryptorules.CryptoRulesDefinition.LANGUAGE_KEY;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public class CryptoQualityProfile implements BuiltInQualityProfilesDefinition {

  private static final String PROFILE_NAME = "Crypto Security";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, LANGUAGE_KEY);
    profile.setDefault(true);

    activateRule(profile, CryptoRulesDefinitions.CC1_GENERAL);
    activateRule(profile, CryptoRulesDefinitions.CC2_ALGORITHM);
    activateRule(profile, CryptoRulesDefinitions.CC5_KEY_LEN);

    profile.done();
  }

  private void activateRule(
      NewBuiltInQualityProfile profile, CryptoRulesDefinition cryptoRulesDefinition) {
    final var ruleKey = cryptoRulesDefinition.getRuleKey();
    profile.activateRule(ruleKey.repository(), ruleKey.rule());
  }
}
