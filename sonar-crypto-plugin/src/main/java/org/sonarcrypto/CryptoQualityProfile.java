package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

@NullMarked
public class CryptoQualityProfile implements BuiltInQualityProfilesDefinition {

  private static final String PROFILE_NAME = "Crypto Security";
  private static final String LANGUAGE_KEY = "java";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, LANGUAGE_KEY);
    profile.setDefault(true);

    profile.activateRule(CryptoRulesDefinition.REPOSITORY_KEY, CryptoRulesDefinition.CC_RULE_NAME);

    profile.done();
  }
}
