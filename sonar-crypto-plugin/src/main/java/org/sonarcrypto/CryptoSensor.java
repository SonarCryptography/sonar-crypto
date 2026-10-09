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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
import org.sonarcrypto.analysis.Outcome;
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

  private static final Ruleset RULESET = Ruleset.JCA_BC_JCA;

  /** Reported for versions missing from {@code versions.properties}. */
  private static final String UNKNOWN_VERSION = "unknown";

  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSensor.class);
  private final CcToSonarIssues issueReporter = new CcToSonarIssues();

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor.name("CogniCryptSensor");
    sensorDescriptor.onlyOnLanguages("java");
  }

  protected RulesetPaths extractRules() throws IOException {
    try {
      return new CryslRuleProvider().extractRulesetToTempDir(RULESET);
    } catch (IOException | URISyntaxException e) {
      final var message =
          String.format(
              "I/O error extracting CrySL rules for ruleset '%s': %s", RULESET, e.getMessage());
      LOGGER.error(message);
      throw new IOException(message, e);
    }
  }

  protected List<ConvertedError> scan(
      FileSystem fileSystem, RulesetPaths extractedRules, CryptoAnalysisInfo info)
      throws FileNotFoundException, MavenBuildException {
    Path jimpleDir = fileSystem.workDir().toPath().resolve("bridge-output/jimple");
    String mavenProjectPath = fileSystem.baseDir().getAbsolutePath();

    final CryptoScanner scanner;

    if (hasJimpleFiles(jimpleDir)) {
      info.put(MetricDefinitions.INPUT_SOURCE, InputSource.JIMPLE);
      LOGGER.info(
          "Using Jimple files from bridge output ({}) as analysis input.",
          jimpleDir.toAbsolutePath());

      var mavenClassPath =
          info.timeAndGet(
              MetricDefinitions.COMPILE_MILLIS, () -> resolveMavenClassPath(mavenProjectPath));
      info.put(MetricDefinitions.CLASSPATH_FALLBACK, mavenClassPath == null);

      var jimpleScanner =
          new JimpleScanner(jimpleDir.toString(), extractedRules.rulesetZip().toString());
      jimpleScanner.setAddClassPath(
          joinClassPaths(extractedRules.dependencyClasspath(), mavenClassPath));

      info.time(MetricDefinitions.ANALYSIS_MILLIS, jimpleScanner::scan);
      scanner = jimpleScanner;
    } else {
      info.put(MetricDefinitions.INPUT_SOURCE, InputSource.MAVEN);
      LOGGER.info(
          "No Jimple files found at {}. Compiling project at {} as analysis input.",
          jimpleDir.toAbsolutePath(),
          mavenProjectPath);

      MavenProject mi = new MavenProject(mavenProjectPath);
      info.time(MetricDefinitions.COMPILE_MILLIS, mi::compile);
      info.put(MetricDefinitions.CLASSPATH_FALLBACK, false);

      HeadlessJavaScanner headlessScanner =
          new HeadlessJavaScanner(mi.getBuildDirectory(), extractedRules.rulesetZip().toString());
      headlessScanner.setFramework(ScannerSettings.Framework.SOOT_UP);
      headlessScanner.setAddClassPath(
          joinClassPaths(
              extractedRules.dependencyClasspath(), Objects.requireNonNull(mi.getFullClassPath())));

      info.time(MetricDefinitions.ANALYSIS_MILLIS, headlessScanner::scan);
      scanner = headlessScanner;
    }

    var errors = new CcErrorConverter(fileSystem).convertErrors(scanner.getCollectedErrors());
    addResultMetrics(info, scanner, errors);
    return errors;
  }

  protected void report(SensorContext sensorContext, List<ConvertedError> errors) {
    LOGGER.info("Found {} cryptographic errors", errors.size());
    issueReporter.reportAllIssues(sensorContext, errors);
  }

  @Override
  public void execute(SensorContext sensorContext) {
    var info = new CryptoAnalysisInfo();
    addRunMetadata(info, sensorContext);
    try {
      info.time(MetricDefinitions.TOTAL_MILLIS, () -> analyze(sensorContext, info));
    } finally {
      LOGGER.info("{}", info);
      writeMetrics(sensorContext, info);
    }
  }

  private void analyze(SensorContext sensorContext, CryptoAnalysisInfo info) {
    final RulesetPaths ruleDir;

    try {
      ruleDir = info.timeAndGet(MetricDefinitions.RULE_EXTRACTION_MILLIS, this::extractRules);
    } catch (IOException e) {
      // Logging is done by `extractRules`.
      info.put(MetricDefinitions.OUTCOME, Outcome.RULE_EXTRACTION_FAILED);
      return;
    }

    // Stays ANALYSIS_FAILED if the scan throws anything other than a build failure.
    var outcome = Outcome.ANALYSIS_FAILED;
    try {
      report(sensorContext, scan(sensorContext.fileSystem(), ruleDir, info));
      outcome = Outcome.SUCCESS;
    } catch (IOException | MavenBuildException e) {
      outcome = Outcome.COMPILE_FAILED;
      LOGGER.error("Failed to build Maven project", e);
    } finally {
      info.put(MetricDefinitions.OUTCOME, outcome);
    }
  }

  private static void addRunMetadata(CryptoAnalysisInfo info, SensorContext sensorContext) {
    var versions = new Properties();
    try (var in = CryptoSensor.class.getResourceAsStream("versions.properties")) {
      if (in != null) {
        versions.load(in);
      }
    } catch (IOException e) {
      LOGGER.warn("Failed to read version information", e);
    }

    info.put(MetricDefinitions.PROJECT_KEY, sensorContext.project().key())
        .put(MetricDefinitions.PLUGIN_VERSION, versions.getProperty("plugin", UNKNOWN_VERSION))
        .put(
            MetricDefinitions.COGNICRYPT_VERSION,
            versions.getProperty("cognicrypt", UNKNOWN_VERSION))
        .put(MetricDefinitions.RULESET, RULESET)
        .put(MetricDefinitions.RULESET_VERSION, versions.getProperty("ruleset", UNKNOWN_VERSION));
  }

  static void writeMetrics(SensorContext sensorContext, CryptoAnalysisInfo info) {
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

  private static void addResultMetrics(
      CryptoAnalysisInfo info, CryptoScanner scanner, List<ConvertedError> errors) {
    var seeds = scanner.getDiscoveredSeeds();
    var classes = new HashSet<String>();
    var methods = new HashSet<String>();
    for (var seed : seeds) {
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

    var errorsPerCryslRule = new HashMap<String, Integer>();
    for (var cellErrors : scanner.getCollectedErrors().values()) {
      for (var error : cellErrors) {
        errorsPerCryslRule.merge(error.getRule().getClassName(), 1, Integer::sum);
      }
    }

    info.put(MetricDefinitions.TOTAL_ERRORS, errors.size())
        .put(MetricDefinitions.ERRORS_PER_RULE_KIND, errorsPerRuleKind)
        .put(MetricDefinitions.ERRORS_PER_CRYSL_RULE, errorsPerCryslRule)
        .put(MetricDefinitions.SEEDS, seeds.size())
        .put(MetricDefinitions.CLASSES_WITH_CRYPTO_USAGE, classes.size())
        .put(MetricDefinitions.METHODS_WITH_CRYPTO_USAGE, methods.size());
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

  /** Returns the project's Maven classpath, or {@code null} if it cannot be resolved. */
  private static @Nullable String resolveMavenClassPath(String mavenProjectPath) {
    try {
      var mavenProject = new MavenProject(mavenProjectPath);
      mavenProject.compile();
      return Objects.requireNonNull(mavenProject.getFullClassPath());
    } catch (IOException | MavenBuildException e) {
      LOGGER.warn(
          "Failed to resolve Maven dependency classpath for {}. Falling back to ruleset dependencies only.",
          mavenProjectPath,
          e);
      return null;
    }
  }

  private static String joinClassPaths(@Nullable String... classPaths) {
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
