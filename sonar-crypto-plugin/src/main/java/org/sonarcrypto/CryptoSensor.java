package org.sonarcrypto;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import org.sonarcrypto.cognicrypt.MavenBuildException;
public class CryptoSensor implements Sensor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSensor.class);

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor.name("CogniCryptSensor");
    sensorDescriptor.onlyOnLanguages("gradlegroovy", "java");
  }

  @Override
  public void execute(SensorContext sensorContext) {
    FileSystem fileSystem = sensorContext.fileSystem();

    String mavenProjectPath =
                new File(fileSystem.baseDir().getAbsolutePath())
                        .getAbsolutePath();
    MavenProject mi;
      try {
          mi = new MavenProject(mavenProjectPath);
          mi.compile();
      } catch (IOException | MavenBuildException e) {
          LOGGER.error("Failed to build project", e);
          return;
      }

      Path ruleDir;
      try {
          CryslRuleProvider ruleProvider = new CryslRuleProvider();
          ruleDir = ruleProvider.extractCryslFilesToTempDir(s -> s.contains("BouncyCastle/"));
      } catch (IOException e) {
          LOGGER.error("Failed to extract Crysl rules", e);
          return;
      }
      HeadlessJavaScanner scanner = new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());
    
    scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
    scanner.scan();
    var errors = scanner.getCollectedErrors();
    LOGGER.info("Errors: {}", errors.size());
  }
}
