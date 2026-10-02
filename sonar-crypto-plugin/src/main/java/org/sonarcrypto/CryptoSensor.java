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
import org.sonarcrypto.analysis.MetricDefinitions;
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

  /** Path of the metrics JSON file; relative paths are resolved against the project base dir. */
  public static final String METRICS_FILE_PROPERTY = "sonar.crypto.metricsFile";

  static final String DEFAULT_METRICS_FILE = "crypto-metrics.json";

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

    var info = new CryptoAnalysisInfo();
    final CryptoScanner scanner;

    if (hasJimpleFiles(jimpleDir)) {
      info.put(MetricDefinitions.INPUT_SOURCE, InputSource.JIMPLE);
      LOGGER.info(
          "Using Jimple files from bridge output ({}) as analysis input.",
          jimpleDir.toAbsolutePath());

      final long compileStart = System.nanoTime();
      String classPath =
          resolveAnalysisClassPath(mavenProjectPath, extractedRules.dependencyClasspath());
      info.put(MetricDefinitions.COMPILE_MILLIS, elapsedMillis(compileStart));

      var jimpleScanner =
          new JimpleScanner(jimpleDir.toString(), extractedRules.rulesetZip().toString());
      jimpleScanner.setAddClassPath(classPath);

      final long analysisStart = System.nanoTime();
      jimpleScanner.scan();
      info.put(MetricDefinitions.ANALYSIS_MILLIS, elapsedMillis(analysisStart));
      scanner = jimpleScanner;
    } else {
      info.put(MetricDefinitions.INPUT_SOURCE, InputSource.MAVEN);
      LOGGER.info(
          "No Jimple files found at {}. Compiling project at {} as analysis input.",
          jimpleDir.toAbsolutePath(),
          mavenProjectPath);

      MavenProject mi = new MavenProject(mavenProjectPath);
      final long compileStart = System.nanoTime();
      mi.compile();
      info.put(MetricDefinitions.COMPILE_MILLIS, elapsedMillis(compileStart));

      HeadlessJavaScanner headlessScanner =
          new HeadlessJavaScanner(mi.getBuildDirectory(), extractedRules.rulesetZip().toString());
      headlessScanner.setFramework(ScannerSettings.Framework.SOOT_UP);
      headlessScanner.setAddClassPath(
          joinClassPaths(
              extractedRules.dependencyClasspath(), Objects.requireNonNull(mi.getFullClassPath())));

      final long analysisStart = System.nanoTime();
      headlessScanner.scan();
      info.put(MetricDefinitions.ANALYSIS_MILLIS, elapsedMillis(analysisStart));
      scanner = headlessScanner;
    }

    var errors = new CcErrorConverter(fileSystem).convertErrors(scanner.getCollectedErrors());
    addResultMetrics(info, scanner, errors);
    return new ScanResult(errors, info);
  }

  protected void report(SensorContext sensorContext, ScanResult result) {
    LOGGER.info("{}", result.info());
    writeMetrics(sensorContext, result.info());
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

  private static void writeMetrics(SensorContext sensorContext, CryptoAnalysisInfo info) {
    var fileSystem = sensorContext.fileSystem();
    var file =
        sensorContext
            .config()
            .get(METRICS_FILE_PROPERTY)
            .map(path -> fileSystem.baseDir().toPath().resolve(path))
            .orElseGet(() -> fileSystem.workDir().toPath().resolve(DEFAULT_METRICS_FILE));
    try {
      info.writeJson(file);
      LOGGER.info("Wrote crypto analysis metrics to {}", file);
    } catch (IOException e) {
      LOGGER.warn("Failed to write crypto analysis metrics to {}", file, e);
    }
  }

  private static long elapsedMillis(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }

  private static void addResultMetrics(
      CryptoAnalysisInfo info, CryptoScanner scanner, List<ConvertedError> errors) {
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

    info.put(MetricDefinitions.TOTAL_ERRORS, errors.size())
        .put(MetricDefinitions.ERRORS_PER_RULE_KIND, errorsPerRuleKind)
        .put(MetricDefinitions.CLASSES_ANALYZED, classes.size())
        .put(MetricDefinitions.METHODS_ANALYZED, methods.size());
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
