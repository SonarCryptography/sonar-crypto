package org.sonarcrypto.utils.test.sonarcontext;

import com.sonarsource.scanner.engine.sensor.test.fixtures.SensorContextTester;
import com.sonarsource.scanner.engine.sensor.test.fixtures.TestInputFileBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.InputFile;

@NullMarked
public class SonarContextTesterUtils {
  private SonarContextTesterUtils() {
    // Utility class
  }

  public static void initializeFileSystem(SensorContextTester context) throws IOException {
    final var fileSystem = context.fileSystem();
    final var baseDir = fileSystem.baseDir().toPath();

    fileSystem.setWorkDir(baseDir);

    try (Stream<Path> fileWalker = Files.walk(baseDir)) {
      fileWalker
          .filter(file -> file.getFileName().toString().endsWith(".java"))
          .map(
              file -> {
                try {
                  return TestInputFileBuilder.create("mod", baseDir.toFile(), file.toFile())
                      .setLanguage("java")
                      .setType(InputFile.Type.MAIN)
                      .setCharset(StandardCharsets.UTF_8)
                      .setContents(Files.readString(file))
                      .build();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              })
          .forEach(fileSystem::add);
    }
  }
}
