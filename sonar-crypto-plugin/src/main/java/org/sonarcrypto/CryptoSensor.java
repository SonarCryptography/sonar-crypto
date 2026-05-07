package org.sonarcrypto;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarcrypto.utils.cognicrypt.crysl.CryslRuleProvider;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;
import org.sonarcrypto.utils.cognicrypt.jimple.JimpleScanner;
import org.sonarcrypto.utils.maven.MavenBuildException;
import org.sonarcrypto.utils.maven.MavenProject;

@NullMarked
@Phase(name = Phase.Name.POST)
public class CryptoSensor implements Sensor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSensor.class);
  private final CcToSonarIssues issueReporter = new CcToSonarIssues();

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor.name("CogniCryptSensor");
    sensorDescriptor.onlyOnLanguages("java");
  }

  @Override
  public void execute(SensorContext sensorContext) {
    FileSystem fileSystem = sensorContext.fileSystem();

    final Ruleset ruleset = Ruleset.BC_JCA;
    Path ruleDir;
    try {
      CryslRuleProvider ruleProvider = new CryslRuleProvider();
      ruleDir = ruleProvider.extractRulesetToTempDir(ruleset);
    } catch (IOException | URISyntaxException e) {
      LOGGER.error(
          "I/O error extracting CrySL rules for ruleset '{}': {}", ruleset, e.getMessage(), e);
      return;
    }

    Table<WrappedClass, Method, Set<AbstractError>> errors;
    Path jimpleDir = fileSystem.workDir().toPath().resolve("bridge-output/jimple");
    if (hasJimpleFiles(jimpleDir)) {
      LOGGER.info(
          "Using Jimple files from bridge output ({}) as analysis input.",
          jimpleDir.toAbsolutePath());
      var scanner = new JimpleScanner(jimpleDir.toString(), ruleDir.toString());
      scanner.scan();
      errors = scanner.getCollectedErrors();
    } else {
      String mavenProjectPath = fileSystem.baseDir().getAbsolutePath();
      LOGGER.info(
          "No Jimple files found at {}. Compiling project at {} as analysis input.",
          jimpleDir.toAbsolutePath(),
          mavenProjectPath);
      MavenProject mi;
      try {
        mi = new MavenProject(mavenProjectPath);
        mi.compile();
      } catch (IOException | MavenBuildException e) {
        LOGGER.error("Failed to build Maven project", e);
        return;
      }
      HeadlessJavaScanner scanner =
          new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());
      scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
      scanner.scan();
      errors = scanner.getCollectedErrors();
    }

    LOGGER.info("Found {} cryptographic errors", errors.size());
    issueReporter.reportAllIssues(sensorContext, errors);
  }

  private static boolean hasJimpleFiles(Path jimpleDir) {
    if (!Files.isDirectory(jimpleDir)) {
      return false;
    }
    try (var stream = Files.walk(jimpleDir)) {
      return stream.anyMatch(p -> p.toString().endsWith(".jimple"));
    } catch (IOException e) {
      return false;
    }
  }
}
