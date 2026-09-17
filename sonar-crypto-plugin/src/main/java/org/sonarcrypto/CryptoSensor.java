package org.sonarcrypto;

import crypto.analysis.CryptoScanner;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarcrypto.analysis.CryptoAnalysisInfo;
import org.sonarcrypto.analysis.InputSource;
import org.sonarcrypto.analysis.ScanResult;
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

  protected ScanResult scan(FileSystem fileSystem, RulesetPaths extractedRules)
      throws FileNotFoundException, MavenBuildException {
    Path jimpleDir = fileSystem.workDir().toPath().resolve("bridge-output/jimple");
    String mavenProjectPath = fileSystem.baseDir().getAbsolutePath();

    final InputSource inputSource;
    final long compileMillis;
    final long analysisMillis;
    final CryptoScanner scanner;

    if (hasJimpleFiles(jimpleDir)) {
      inputSource = InputSource.JIMPLE;
      LOGGER.info(
          "Using Jimple files from bridge output ({}) as analysis input.",
          jimpleDir.toAbsolutePath());

      final long compileStart = System.nanoTime();
      String classPath =
          resolveAnalysisClassPath(mavenProjectPath, extractedRules.dependencyClasspath());
      compileMillis = elapsedMillis(compileStart);

      var jimpleScanner =
          new JimpleScanner(jimpleDir.toString(), extractedRules.rulesetZip().toString());
      jimpleScanner.setAddClassPath(classPath);

      final long analysisStart = System.nanoTime();
      jimpleScanner.scan();
      analysisMillis = elapsedMillis(analysisStart);
      scanner = jimpleScanner;
    } else {
      inputSource = InputSource.MAVEN;
      LOGGER.info(
          "No Jimple files found at {}. Compiling project at {} as analysis input.",
          jimpleDir.toAbsolutePath(),
          mavenProjectPath);

      MavenProject mi = new MavenProject(mavenProjectPath);
      final long compileStart = System.nanoTime();
      mi.compile();
      compileMillis = elapsedMillis(compileStart);

      HeadlessJavaScanner headlessScanner =
          new HeadlessJavaScanner(mi.getBuildDirectory(), extractedRules.rulesetZip().toString());
      headlessScanner.setFramework(ScannerSettings.Framework.SOOT_UP);
      headlessScanner.setAddClassPath(
          joinClassPaths(
              extractedRules.dependencyClasspath(), Objects.requireNonNull(mi.getFullClassPath())));

      final long analysisStart = System.nanoTime();
      headlessScanner.scan();
      analysisMillis = elapsedMillis(analysisStart);
      scanner = headlessScanner;
    }

    var errors = new CcErrorConverter(fileSystem).convertErrors(scanner.getCollectedErrors());
    return new ScanResult(
        errors, buildInfo(inputSource, compileMillis, analysisMillis, scanner, errors));
  }

  protected void report(SensorContext sensorContext, ScanResult result) {
    LOGGER.info("{}", result.info());
    issueReporter.reportAllIssues(sensorContext, result.errors());
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

    try {
      report(sensorContext, scan(sensorContext.fileSystem(), ruleDir));
    } catch (IOException | MavenBuildException e) {
      LOGGER.error("Failed to build Maven project", e);
    }
  }

  private static long elapsedMillis(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }

  private static CryptoAnalysisInfo buildInfo(
      InputSource inputSource,
      long compileMillis,
      long analysisMillis,
      CryptoScanner scanner,
      List<ConvertedError> errors) {
    var classes = new HashSet<String>();
    var methods = new HashSet<String>();
    for (var seed : scanner.getDiscoveredSeeds()) {
      var method = seed.getMethod();
      var declaringClass = method.getDeclaringClass().getFullyQualifiedName();
      classes.add(declaringClass);
      methods.add(declaringClass + "#" + method.getSubSignature());
    }

    var errorsPerRuleKind = new EnumMap<RuleKind, Integer>(RuleKind.class);
    for (var error : errors) {
      errorsPerRuleKind.merge(
          error.violation().getRulesDefinition().getRuleKind(), 1, Integer::sum);
    }

    return new CryptoAnalysisInfo(
        inputSource,
        compileMillis,
        analysisMillis,
        errors.size(),
        errorsPerRuleKind,
        classes.size(),
        methods.size());
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
      return joinClassPaths(
          rulesetDependencyClasspath, Objects.requireNonNull(mavenProject.getFullClassPath()));
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
      if (!joiner.isEmpty()) {
        joiner.append(File.pathSeparator);
      }
      joiner.append(classPath.trim());
    }

    return joiner.toString();
  }
}
