package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.Plugin;

@NullMarked
public class CryptoPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtension(CryptoRulesDefinitions.CC1_GENERAL);
    context.addExtension(CryptoRulesDefinitions.CC2_ALGORITHM);
    context.addExtension(CryptoRulesDefinitions.CC5_KEY_LEN);
    context.addExtension(CryptoRulesDefinitions.CC6_FORBIDDEN_TYPE);
    context.addExtension(CryptoQualityProfile.class);
    context.addExtension(CryptoSensor.class);
  }
}
