package org.sonarcrypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;

public class CryptoPlugin implements Plugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoPlugin.class);

  @Override
  public void define(Context context) {
    LOGGER.info(" ----> Registering CogniCrypto Sensor");
    LOGGER.info(" ----> Plugin context: {}", context);
    context.addExtension(CryptoSensor.class);
    LOGGER.info(" ----> CogniCrypto Sensor registered successfully");
  }
}
