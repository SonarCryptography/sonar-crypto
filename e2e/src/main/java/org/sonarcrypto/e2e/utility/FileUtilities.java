package org.sonarcrypto.e2e.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtilities {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilities.class);

  public static File findFile(String path, String fileName, String fileEnding) {
    return findFile(Path.of(path), fileName, fileEnding);
  }

  public static File findFile(Path path, String fileName, String fileEnding) {
    if (path == null || !Files.exists(path)) {
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
    } catch (Exception e) {
      LOGGER.error("Error while searching for file: {}*{}", fileName, fileEnding, e);
      return null;
    }
  }
}
