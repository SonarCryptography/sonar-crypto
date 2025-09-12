package org.sonarcrypto;

import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSensor.class);

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor.name("CogniCryptSensor");
    sensorDescriptor.onlyOnLanguages("java");
  }


  @Override
  public void execute(SensorContext sensorContext) {
    LOGGER.info(" ----> Executing CogniCryptSensor");
    LOGGER.info(" ----> Sensor context: {}", sensorContext);
    
    FileSystem fileSystem = sensorContext.fileSystem();
    LOGGER.info(" ----> File system base directory: {}", fileSystem.baseDir().getAbsolutePath());
    
    // Check if there are any Java files
    Iterable<org.sonar.api.batch.fs.InputFile> javaFiles = fileSystem.inputFiles(fileSystem.predicates().hasLanguage("java"));
    int javaFileCount = 0;
    for (org.sonar.api.batch.fs.InputFile file : javaFiles) {
      javaFileCount++;
      LOGGER.info(" ----> Found Java file: {}", file.uri());
    }
    LOGGER.info(" ----> Total Java files found: {}", javaFileCount);

    String mavenProjectPath = fileSystem.baseDir().getAbsolutePath();
    LOGGER.info(" ----> Maven project path: {}", mavenProjectPath);
    
    MavenProject mi;
    try {
      LOGGER.info(" ----> Creating MavenProject instance");
      mi = new MavenProject(mavenProjectPath);
      LOGGER.info(" ----> Attempting to compile Maven project");
      mi.compile();
      LOGGER.info(" ----> Maven project compiled successfully");
    } catch (IOException | MavenBuildException e) {
      LOGGER.warn(" ----> Failed to build Maven project via Maven invoker: {}", e.getMessage());
      LOGGER.info(" ----> Exception type: {}", e.getClass().getName());
      LOGGER.info(" ----> Attempting to use existing compiled classes");
      
      // Check if target/classes exists and has compiled classes
      File buildDir = new File(mavenProjectPath + File.separator + "target" + File.separator + "classes");
      if (buildDir.exists() && buildDir.isDirectory()) {
        LOGGER.info(" ----> Found existing compiled classes in: {}", buildDir.getAbsolutePath());
        // Use existing compiled classes without Maven compilation
        try {
          mi = new MavenProject(mavenProjectPath);
          // Manually set the compiled state
          java.lang.reflect.Field compiledField = MavenProject.class.getDeclaredField("compiled");
          compiledField.setAccessible(true);
          compiledField.setBoolean(mi, true);
          
          java.lang.reflect.Field classPathField = MavenProject.class.getDeclaredField("fullProjectClassPath");
          classPathField.setAccessible(true);
          classPathField.set(mi, ""); // Empty classpath for now
          
          LOGGER.info(" ----> Using existing compiled classes");
        } catch (Exception reflectionException) {
          LOGGER.error(" ----> Failed to set compiled state via reflection", reflectionException);
          return;
        }
      } else {
        LOGGER.error(" ----> No compiled classes found and Maven compilation failed");
        return;
      }
    }

    Path ruleDir;
    try {
      LOGGER.info(" ----> Creating CryslRuleProvider and extracting rules");
      CryslRuleProvider ruleProvider = new CryslRuleProvider();
      ruleDir = ruleProvider.extractCryslFilesToTempDir(s -> s.contains("BouncyCastle/"));
      LOGGER.info(" ----> Rules extracted to: {}", ruleDir);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn(" ----> Extraction interrupted while filtering 'BouncyCastle/'", e);
      return; // or rethrow if upstream should handle
    } catch (IOException e) {
      LOGGER.error(
          " ----> I/O error extracting Crysl rules for filter 'BouncyCastle/': {}", e.getMessage(), e);
      return;
    }

    LOGGER.info(" ----> Creating HeadlessJavaScanner");
    HeadlessJavaScanner scanner =
        new HeadlessJavaScanner(mi.getBuildDirectory(), Path.of(ruleDir.toString(), "BouncyCastle", "src").toAbsolutePath().toString());

    LOGGER.info(" ----> Setting framework to SOOT_UP");
    scanner.setFramework(ScannerSettings.Framework.SOOT);
    
    LOGGER.info(" ----> Starting scan");
    scanner.scan();
    
    var errors = scanner.getCollectedErrors();
    LOGGER.info(" ----> Scan completed. Errors found: {}", errors.size());
    
    if (!errors.isEmpty()) {
      LOGGER.info(" ----> Error details:");
      errors.cellSet().forEach(cell -> {
        LOGGER.info(" ---->   - Class: {}, Method: {}, Error: {}", 
                   cell.getRowKey(), cell.getColumnKey(), cell.getValue());
      });
    }
    
    LOGGER.info(" ----> CogniCryptSensor execution completed");
  }
}
