package org.sonarcrypto;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
public class CryptoSensor implements Sensor {

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
    MavenProject mi = new MavenProject(mavenProjectPath);
    mi.compile();

      Path ruleDir = null;
      try {
          // TODO: make this configurable in SonarCloud
          CryslRuleProvider ruleProvider = new CryslRuleProvider();
          ruleDir = ruleProvider.extractCryslFilesToTempDir(s -> s.contains("BouncyCastle/"));
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
      HeadlessJavaScanner scanner = new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());
    
    scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
    scanner.scan();
    var errors = scanner.getCollectedErrors();
    System.out.println("Errors: " + errors.size());
  }
}
