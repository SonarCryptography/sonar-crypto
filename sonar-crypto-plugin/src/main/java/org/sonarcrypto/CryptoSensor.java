package org.sonarcrypto;

import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarcrypto.cognicrypt.MavenBuildException;
import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;
import org.sonarcrypto.scanner.CryptoAnalysisScanner;

@NullMarked
public class CryptoSensor implements Sensor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSensor.class);

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor.name("CogniCryptSensor");
    sensorDescriptor.onlyOnLanguages("java");
  }

  @Override
  public void execute(SensorContext sensorContext) {
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
    
    final String ruleset = "bc";
    Path ruleDir;
    try {
      CryslRuleProvider ruleProvider = new CryslRuleProvider();
      ruleDir = ruleProvider.extractRulesetToTempDir(ruleset);
    } catch (IOException | URISyntaxException e) {
      LOGGER.error(
          "I/O error extracting CrySL rules for ruleset '{}': {}", ruleset, e.getMessage(), e);
      return;
    }
    
    CryptoAnalysisScanner scanner =
        new CryptoAnalysisScanner(mi.getBuildDirectory(), ruleDir.toString());

    scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
    scanner.scan();
    var errors = scanner.getCollectedErrors();
    LOGGER.info("Errors: {}", errors.size());
  }
}
