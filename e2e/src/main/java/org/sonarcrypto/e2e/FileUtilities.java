package org.sonarcrypto.e2e;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtilities {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilities.class);

  static File sonarCryptoJar(String buildDirPath) {
    File buildDir = new File(buildDirPath);
    return sonarCryptoJar(buildDir);
  }

  // Visible for testing
  static File sonarCryptoJar(File buildDir) {
    if (!buildDir.exists()) {
      LOGGER.error("Build directory does not exist: {}", buildDir.getAbsolutePath());
      return null;
    }
    try {
      buildDir = buildDir.getCanonicalFile();
    } catch (IOException e) {
      LOGGER.error(
          "Could not resolve canonical path for build directory: {}", buildDir.getAbsolutePath());
      return null;
    }
    File[] candidates = buildDir.listFiles();
    if (candidates != null) {
      for (File candidate : candidates) {
        if (candidate.getName().startsWith("sonar-crypto-plugin")
            && candidate.getName().endsWith(".jar")) {
          return candidate;
        }
      }
    }
    LOGGER.error(
        "Could not find sonar-crypto-plugin jar in build directory: {}",
        buildDir.getAbsolutePath());
    return null;
  }
}
