package org.sonarcrypto;

import org.sonar.api.Plugin;

public class CryptoPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtension(CryptoSensor.class);
  }
}
