package org.sonarcrypto;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonarcrypto.cognicrypt.MavenBuildException;
import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;

@NullMarked
public class CryptoSensor implements Sensor {

  @FunctionalInterface
  interface AnalysisRunner {
    Table<WrappedClass, Method, Set<AbstractError>> analyze(String projectPath) throws Exception;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSensor.class);
  private final CcToSonarIssues issueReporter;
  private final AnalysisRunner analysisRunner;

  public CryptoSensor() {
    this(new CcToSonarIssues(), CryptoSensor::defaultAnalysis);
  }

  CryptoSensor(CcToSonarIssues issueReporter, AnalysisRunner analysisRunner) {
    this.issueReporter = issueReporter;
    this.analysisRunner = analysisRunner;
  }

  @Override
  public void describe(SensorDescriptor sensorDescriptor) {
    sensorDescriptor.name("CogniCryptSensor");
    sensorDescriptor.onlyOnLanguages("java");
  }

  @Override
  public void execute(SensorContext sensorContext) {
    String projectPath = sensorContext.fileSystem().baseDir().getAbsolutePath();

    Table<WrappedClass, Method, Set<AbstractError>> errors;
    try {
      errors = analysisRunner.analyze(projectPath);
    } catch (Exception e) {
      LOGGER.error("Cryptographic analysis failed", e);
      return;
    }

    LOGGER.info("Found {} cryptographic errors", errors.size());
    issueReporter.reportAllIssues(sensorContext, errors);
  }

  private static Table<WrappedClass, Method, Set<AbstractError>> defaultAnalysis(String projectPath)
      throws IOException, MavenBuildException, URISyntaxException {
    MavenProject mi = new MavenProject(projectPath);
    mi.compile();

    final String ruleset = "bc";
    CryslRuleProvider ruleProvider = new CryslRuleProvider();
    Path ruleDir = ruleProvider.extractRulesetToTempDir(ruleset);

    HeadlessJavaScanner scanner =
        new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());

    scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
    scanner.scan();
    return scanner.getCollectedErrors();
  }
}
