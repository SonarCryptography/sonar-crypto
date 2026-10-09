package org.sonarcrypto;

import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.ConfigScope;

@NullMarked
public class CryptoPlugin implements Plugin {

  @Override
  public void define(Context context) {
    Arrays.stream(RuleKind.values())
        .map(CryptoRulesDefinitions::fromRuleKind)
        .forEach(context::addExtension);

    context.addExtension(CryptoQualityProfile.class);
    context.addExtension(CryptoSensor.class);
    context.addExtension(
        PropertyDefinition.builder(CryptoSensor.METRICS_FILE_PROPERTY)
            .name("Metrics file")
            .description(
                "Path of the JSON file the crypto analysis metrics are written to. Relative paths"
                    + " are resolved against the project base directory. Defaults to "
                    + CryptoSensor.DEFAULT_METRICS_FILE
                    + " in the scanner working directory.")
            .category("Crypto")
            .onConfigScopes(ConfigScope.PROJECT)
            .build());
  }
}
