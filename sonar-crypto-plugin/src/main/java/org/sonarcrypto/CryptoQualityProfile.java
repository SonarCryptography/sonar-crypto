package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarcrypto.utility.FileUtilities;

@NullMarked
public class CryptoQualityProfile implements BuiltInQualityProfilesDefinition {
  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoQualityProfile.class);

  private static final String PROFILE_NAME = "Crypto Security";
  private static final String LANGUAGE_KEY = "java";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile =
        context.createBuiltInQualityProfile(PROFILE_NAME, LANGUAGE_KEY);
    profile.setDefault(true);

    if (areSonarPrivatePluginsAvailable()) {
      LOGGER.debug(
          "Sonar private plugins are available. Activating arbitrary javasecurity rule (S3649) to trigger the sonar-security-java-frontend-plugin.");
      profile.activateRule("javasecurity", "S3649");
    }

    profile.activateRule(CryptoRulesDefinition.REPOSITORY_KEY, CryptoRulesDefinition.CC_RULE_NAME);

    profile.done();
  }

  private static boolean areSonarPrivatePluginsAvailable() {
    return FileUtilities.areSonarPrivatePluginsAvailable(
        "../../../src/test/resources/SonarPrivatePlugins");
  }
}
