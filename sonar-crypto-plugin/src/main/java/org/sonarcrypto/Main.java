package org.sonarcrypto;

import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.File;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.cognicrypt.MavenBuildException;
import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;

public class Main {
  private static final @NonNull Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static void main(@NonNull String @NonNull [] args) throws Exception {
    CryslRuleProvider ruleProvider = new CryslRuleProvider();
    Path ruleDir =
        ruleProvider.extractCryslFilesToTempDir(s -> s.contains("JavaCryptographicArchitecture/"));
    String mavenProjectPath = new File(args[0]).getAbsolutePath();
    try {
      MavenProject mi = new MavenProject(mavenProjectPath);
      mi.compile();
      LOGGER.info("Built project to directory: {}", mi.getBuildDirectory());
      HeadlessJavaScanner scanner =
          new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());

      scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
      scanner.scan();
      var errors = scanner.getCollectedErrors();
      LOGGER.info("Errors: {}", errors.size());
    } catch (MavenBuildException e) {
      LOGGER.error("Failed to build project", e);
    }
  }
}
