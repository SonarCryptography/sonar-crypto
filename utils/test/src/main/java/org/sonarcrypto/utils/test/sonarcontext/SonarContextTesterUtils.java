package org.sonarcrypto.utils.test.sonarcontext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

@NullMarked
public class SonarContextTesterUtils {

  public static void initializeFileSystem(SensorContextTester context) throws IOException {
    final var fileSystem = context.fileSystem();
    final var baseDir = fileSystem.baseDirPath();

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

  private InputFile addJavaFile(
      SensorContextTester sensorContext, Path baseDir, String relativePath, String content)
      throws IOException {
    Path srcDir = baseDir.resolve("src/main/java");
    Files.createDirectories(srcDir);
    Path javaFile = srcDir.resolve(relativePath);
    Files.createDirectories(javaFile.getParent());
    Files.writeString(javaFile, content);

    InputFile inputFile =
        TestInputFileBuilder.create("mod", baseDir.toFile(), javaFile.toFile())
            .setLanguage("java")
            .setType(InputFile.Type.MAIN)
            .setCharset(StandardCharsets.UTF_8)
            .setContents(content)
            .build();
    sensorContext.fileSystem().add(inputFile);
    return inputFile;
  }
}
