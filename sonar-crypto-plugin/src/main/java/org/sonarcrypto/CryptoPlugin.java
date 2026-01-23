package org.sonarcrypto;

import org.jspecify.annotations.NonNull;
import org.sonar.api.Plugin;

public class CryptoPlugin implements Plugin {

  @Override
  public void define(@NonNull Context context) {
    context.addExtension(CryptoSensor.class);
  }
}
