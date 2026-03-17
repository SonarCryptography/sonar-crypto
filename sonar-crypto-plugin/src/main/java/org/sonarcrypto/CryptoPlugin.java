package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.Plugin;

@NullMarked
public class CryptoPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtension(CryptoRulesDefinitions.CC1_OI);
    context.addExtension(CryptoRulesDefinitions.CC2_UA);
    context.addExtension(CryptoRulesDefinitions.CC3_KL);
    context.addExtension(CryptoQualityProfile.class);
    context.addExtension(CryptoSensor.class);
  }
}
