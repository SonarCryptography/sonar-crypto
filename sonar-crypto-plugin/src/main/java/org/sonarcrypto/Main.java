package org.sonarcrypto;

import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.cognicrypt.MavenBuildException;
import org.sonarcrypto.cognicrypt.MavenProject;

public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    // Use direct filesystem path to CrySL rules instead of resource-based approach
    Path ruleDir = Paths.get("sonar-crypto-plugin/target/classes/crysl-rules/JavaCryptographicArchitecture/src");
    if (!Files.exists(ruleDir)) {
      LOGGER.error("CrySL rules directory not found at: {}", ruleDir.toAbsolutePath());
      return;
    }
    LOGGER.info(" ----> Using CrySL rules from: {}", ruleDir.toAbsolutePath());
    String mavenProjectPath = new File("crypto-test-project").getAbsolutePath();
    
    try {
      MavenProject mavenProject = new MavenProject(mavenProjectPath);
      mavenProject.compile();
      LOGGER.info(" ----> Built project to directory: {}", mavenProject.getBuildDirectory());
      
      HeadlessJavaScanner scanner =
          new HeadlessJavaScanner(mavenProject.getBuildDirectory(), ruleDir.toString());

      scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
      scanner.scan();
      var errors = scanner.getCollectedErrors();
      LOGGER.info(" ----> Errors: {}", errors.size());
    } catch (MavenBuildException e) {
      LOGGER.error(" ----> Failed to build project", e);
    } catch (Exception e) {
      LOGGER.error(" ----> Failed to scan project", e);
    }
  }
}
