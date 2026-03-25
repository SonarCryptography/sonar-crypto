package org.sonarcrypto;

import static org.sonarcrypto.cryptorules.CryptoRulesDefinition.LANGUAGE_KEY;

import java.util.Arrays;
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

    Arrays.stream(RuleKind.values())
        .map(CryptoRulesDefinitions::fromRuleKind)
        .forEach(it -> activateRule(profile, it));

    profile.done();
  }

  private void activateRule(
      NewBuiltInQualityProfile profile, CryptoRulesDefinition cryptoRulesDefinition) {
    final var ruleKey = cryptoRulesDefinition.getRuleKey();
    profile.activateRule(ruleKey.repository(), ruleKey.rule());
  }
}
