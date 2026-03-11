package org.sonarcrypto;

import static org.sonarcrypto.cryptorules.CryptoRulesDefinition.LANGUAGE_KEY;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

@NullMarked
public class CryptoQualityProfile implements BuiltInQualityProfilesDefinition {

  private static final String PROFILE_NAME = "Crypto Security";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, LANGUAGE_KEY);
    profile.setDefault(true);

    final var ruleKey = CryptoRulesDefinitions.CC1.getRuleKey();
    profile.activateRule(ruleKey.repository(), ruleKey.rule());

    profile.done();
  }
}
