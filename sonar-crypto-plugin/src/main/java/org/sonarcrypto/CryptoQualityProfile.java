package org.sonarcrypto;

import static org.sonarcrypto.cryptorules.CryptoRulesDefinition.LANGUAGE_KEY;

import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utility.FileUtilities;

@NullMarked
public class CryptoQualityProfile implements BuiltInQualityProfilesDefinition {
  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoQualityProfile.class);

  private static final String PROFILE_NAME = "Crypto Security";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, LANGUAGE_KEY);
    profile.setDefault(true);

    if (areSonarPrivatePluginsAvailable()) {
      LOGGER.debug(
          "Sonar private plugins are available. Activating arbitrary javasecurity rule "
              + "(S3649) to trigger the sonar-security-java-frontend-plugin.");
      profile.activateRule("javasecurity", "S3649");
    }

    Arrays.stream(RuleKind.values())
        .map(CryptoRulesDefinitions::fromRuleKind)
        .forEach(it -> activateRule(profile, it));

    profile.done();
  }

  private static boolean areSonarPrivatePluginsAvailable() {
    return FileUtilities.areSonarPrivatePluginsAvailable(
        "../../../src/test/resources/SonarPrivatePlugins");
  }

  private void activateRule(
      NewBuiltInQualityProfile profile, CryptoRulesDefinition cryptoRulesDefinition) {
    final var ruleKey = cryptoRulesDefinition.getRuleKey();
    profile.activateRule(ruleKey.repository(), ruleKey.rule());
  }
}
