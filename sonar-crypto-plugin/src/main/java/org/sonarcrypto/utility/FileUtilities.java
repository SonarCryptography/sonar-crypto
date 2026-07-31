package org.sonarcrypto.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class FileUtilities {
  private FileUtilities() {
    // Private constructor to prevent instantiation
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilities.class);

  public static final String SONAR_SECURITY_JAVA_FRONTEND = "sonar-security-java-frontend-plugin";
  public static final String SONAR_SECURITY_UCFG_BRIDGE = "sonar-security-ucfg-bridge";

  public static @Nullable File findFile(String path, String fileName, String fileEnding) {
    return findFile(Path.of(path), fileName, fileEnding);
  }

  public static @Nullable File findFile(Path path, String fileName, String fileEnding) {
    if (!Files.exists(path)) {
      LOGGER.error("Cannot search for file on non-existent path: {}", path.toAbsolutePath());
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
