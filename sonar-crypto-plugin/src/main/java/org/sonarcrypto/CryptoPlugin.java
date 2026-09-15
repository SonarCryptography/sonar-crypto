package org.sonarcrypto;

import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.Plugin;
import org.sonarcrypto.analysis.CryptoMetrics;

@NullMarked
public class CryptoPlugin implements Plugin {

  @Override
  public void define(Context context) {
    Arrays.stream(RuleKind.values())
        .map(CryptoRulesDefinitions::fromRuleKind)
        .forEach(context::addExtension);

    context.addExtension(CryptoQualityProfile.class);
    context.addExtension(CryptoSensor.class);
    if (CryptoMetrics.isEnabled()) {
      context.addExtension(CryptoMetrics.class);
    }
  }
}
