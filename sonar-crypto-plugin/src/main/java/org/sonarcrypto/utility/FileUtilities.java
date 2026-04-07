package org.sonarcrypto.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtilities {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilities.class);

  public static final String SONAR_SECURITY_JAVA_FRONTEND = "sonar-security-java-frontend-plugin";
  public static final String SONAR_SECURITY_UCFG_BRIDGE = "sonar-security-ucfg-bridge";

  public static File findFile(String path, String fileName, String fileEnding) {
    return findFile(Path.of(path), fileName, fileEnding);
  }

  public static File findFile(Path path, String fileName, String fileEnding) {
    if (path == null || !Files.exists(path)) {
      if (path != null) {
        LOGGER.error("Cannot search for file on non-existent path: {}", path.toAbsolutePath());
      }
      return null;
    }
    try (Stream<Path> fileWalker = Files.walk(path)) {
      return fileWalker
          .filter(Files::isRegularFile)
          .map(Path::toFile)
          .filter(
              file -> {
                String name = file.getName();
                return name.startsWith(fileName) && name.endsWith(fileEnding);
              })
          .findFirst()
          .orElseThrow(FileNotFoundException::new);
    } catch (FileNotFoundException e) {
      LOGGER.error("Error (file not found) while searching for file: {}*{}", fileName, fileEnding);
      return null;
    } catch (Exception e) {
      LOGGER.error("Unexpected error while searching for file: {}*{}", fileName, fileEnding, e);
      return null;
    }
  }

  public static boolean areSonarPrivatePluginsAvailable(String sonarPrivatePluginsDir) {
    return findFile(sonarPrivatePluginsDir, SONAR_SECURITY_JAVA_FRONTEND, ".jar") != null
        && findFile(sonarPrivatePluginsDir, SONAR_SECURITY_UCFG_BRIDGE, ".jar") != null;
  }
}
