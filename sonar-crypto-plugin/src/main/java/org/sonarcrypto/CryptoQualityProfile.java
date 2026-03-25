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

    activateRule(profile, CryptoRulesDefinitions.GENERAL);
    activateRule(profile, CryptoRulesDefinitions.ALGORITHM);
    activateRule(profile, CryptoRulesDefinitions.KEY_LENGTH);
    activateRule(profile, CryptoRulesDefinitions.FORBIDDEN_TYPE);

    profile.done();
  }

  private void activateRule(
      NewBuiltInQualityProfile profile, CryptoRulesDefinition cryptoRulesDefinition) {
    final var ruleKey = cryptoRulesDefinition.getRuleKey();
    profile.activateRule(ruleKey.repository(), ruleKey.rule());
  }
}
