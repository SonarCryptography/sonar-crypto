package org.sonarcrypto;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarcrypto.ccerror.CcErrorConverter;
import org.sonarcrypto.ccerror.ConvertedError;
import org.sonarcrypto.utils.cognicrypt.crysl.CryslRuleProvider;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;
import org.sonarcrypto.utils.cognicrypt.crysl.RulesetPaths;
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

  protected RulesetPaths extractRules() throws IOException {
    final Ruleset ruleset = Ruleset.JCA_BC_JCA;
    try {
      return new CryslRuleProvider().extractRulesetToTempDir(ruleset);
    } catch (IOException | URISyntaxException e) {
      final var message =
          String.format(
              "I/O error extracting CrySL rules for ruleset '%s': %s", ruleset, e.getMessage());
      LOGGER.error(message);
      throw new IOException(message, e);
    }
  }

  protected List<ConvertedError> scan(FileSystem fileSystem, RulesetPaths extractedRules) {
    Table<WrappedClass, Method, Set<AbstractError>> errors;
    Path jimpleDir = fileSystem.workDir().toPath().resolve("bridge-output/jimple");
    String mavenProjectPath = fileSystem.baseDir().getAbsolutePath();
    if (hasJimpleFiles(jimpleDir)) {
      LOGGER.info(
          "Using Jimple files from bridge output ({}) as analysis input.",
          jimpleDir.toAbsolutePath());
      var scanner = new JimpleScanner(jimpleDir.toString(), extractedRules.rulesetZip().toString());
      scanner.setAddClassPath(
          resolveAnalysisClassPath(mavenProjectPath, extractedRules.dependencyClasspath()));
      scanner.scan();
      errors = scanner.getCollectedErrors();
    } else {
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
        return List.of(/* Empty */ );
      }
      HeadlessJavaScanner scanner =
          new HeadlessJavaScanner(mi.getBuildDirectory(), extractedRules.rulesetZip().toString());
      scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
      scanner.setAddClassPath(
          joinClassPaths(extractedRules.dependencyClasspath(), mi.getFullClassPath()));
      scanner.scan();
      errors = scanner.getCollectedErrors();
    }

    return new CcErrorConverter(fileSystem).convertErrors(errors);
  }

  protected void report(SensorContext sensorContext, List<ConvertedError> errors) {
    LOGGER.info("Found {} cryptographic errors", errors.size());
    issueReporter.reportAllIssues(sensorContext, errors);
  }

  @Override
  public void execute(SensorContext sensorContext) {
    final RulesetPaths ruleDir;

    try {
      ruleDir = extractRules();
    } catch (IOException e) {
      // Logging is done by `extractRules`.
      return;
    }

    report(sensorContext, scan(sensorContext.fileSystem(), ruleDir));
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

  private static String resolveAnalysisClassPath(
      String mavenProjectPath, String rulesetDependencyClasspath) {
    try {
      var mavenProject = new MavenProject(mavenProjectPath);
      mavenProject.compile();
      return joinClassPaths(rulesetDependencyClasspath, mavenProject.getFullClassPath());
    } catch (IOException | MavenBuildException e) {
      LOGGER.warn(
          "Failed to resolve Maven dependency classpath for {}. Falling back to ruleset dependencies only.",
          mavenProjectPath,
          e);
      return rulesetDependencyClasspath;
    }
  }

  private static String joinClassPaths(String... classPaths) {
    final var joiner = new StringBuilder();

    for (var classPath : classPaths) {
      if (classPath == null || classPath.isBlank()) {
        continue;
      }
      if (joiner.length() > 0) {
        joiner.append(File.pathSeparator);
      }
      joiner.append(classPath.trim());
    }

    return joiner.toString();
  }
}
