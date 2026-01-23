package org.sonarcrypto;

import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.IOException;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarcrypto.cognicrypt.MavenBuildException;
import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;

public class CryptoSensor implements Sensor {

  private static final @NonNull Logger LOGGER = LoggerFactory.getLogger(CryptoSensor.class);

  @Override
  public void describe(@NonNull SensorDescriptor sensorDescriptor) {
    sensorDescriptor.name("CogniCryptSensor");
    sensorDescriptor.onlyOnLanguages("java");
  }

  @Override
  public void execute(@NonNull SensorContext sensorContext) {
    FileSystem fileSystem = sensorContext.fileSystem();

    String mavenProjectPath = fileSystem.baseDir().getAbsolutePath();
    MavenProject mi;
    try {
      mi = new MavenProject(mavenProjectPath);
      mi.compile();
    } catch (IOException | MavenBuildException e) {
      LOGGER.error("Failed to build Maven project", e);
      return;
    }

    Path ruleDir;
    try {
      CryslRuleProvider ruleProvider = new CryslRuleProvider();
      ruleDir = ruleProvider.extractCryslFilesToTempDir(s -> s.contains("BouncyCastle/"));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Extraction interrupted while filtering 'BouncyCastle/'", e);
      return; // or rethrow if upstream should handle
    } catch (IOException e) {
      LOGGER.error(
          "I/O error extracting Crysl rules for filter 'BouncyCastle/': {}", e.getMessage(), e);
      return;
    }

    HeadlessJavaScanner scanner =
        new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());

    scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
    scanner.scan();
    var errors = scanner.getCollectedErrors();
    LOGGER.info("Errors: {}", errors.size());
  }
}
