package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.Plugin;

@NullMarked
public class CryptoPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtension(CryptoRulesDefinitions.GENERAL);
    context.addExtension(CryptoRulesDefinitions.ALGORITHM);
    context.addExtension(CryptoRulesDefinitions.MODE);
    context.addExtension(CryptoRulesDefinitions.PADDING);
    context.addExtension(CryptoRulesDefinitions.KEY_LENGTH);
    context.addExtension(CryptoRulesDefinitions.FORBIDDEN_TYPE);
    context.addExtension(CryptoQualityProfile.class);
    context.addExtension(CryptoSensor.class);
  }
}
