package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.Plugin;

@NullMarked
public class CryptoPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtension(CryptoRulesDefinition.class);
    context.addExtension(CryptoQualityProfile.class);
    context.addExtension(CryptoSensor.class);
  }
}
