package org.sonarcrypto.utils.cognicrypt.jimple;

import boomerang.scope.DataFlowScope;
import crypto.analysis.CryptoAnalysisDataFlowScope;
import crypto.analysis.CryptoScanner;
import crypto.exceptions.CryptoAnalysisException;
import crypto.reporting.Reporter;
import crypto.reporting.ReporterFactory;
import crypto.visualization.Visualizer;
import crysl.rule.CrySLRule;
import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisScope;
import de.fraunhofer.iem.framework.FrameworkSetup;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.IOException;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JimpleScanner extends CryptoScanner {

  private static final Logger LOGGER = LoggerFactory.getLogger(JimpleScanner.class);

  private final ScannerSettings settings;

  public JimpleScanner(String applicationPath, String rulesetDirectory) {
    settings = new ScannerSettings();

    settings.setApplicationPath(applicationPath);
    settings.setRulesetPath(rulesetDirectory);
  }

  public void scan() {
    LOGGER.info("Reading rules from {}", settings.getRulesetPath());
    Collection<CrySLRule> rules = readRules(settings.getRulesetPath(), settings.getAddClassPath());
    LOGGER.info("Found {} rules in {}", rules.size(), settings.getRulesetPath());

    // Initialize the reporters before the analysis to catch errors early
    Collection<Reporter> reporters =
        ReporterFactory.createReporters(
            settings.getReportFormats(), settings.getReportDirectory(), rules);
    if (settings.isVisualization() && settings.getReportDirectory() == null) {
      throw new IllegalArgumentException(
          "Cannot create visualization without existing report directory");
    }
    // Set up the framework
    DataFlowScope dataFlowScope =
        new CryptoAnalysisDataFlowScope(rules, settings.getIgnoredSections());
    CryptoAnalysisScope frameworkScope = initializeFramework(dataFlowScope);
    super.scan(frameworkScope, rules, settings.getAddClassPath());

    // Report the errors
    for (Reporter reporter : reporters) {
      reporter.createAnalysisReport(
          super.getDiscoveredSeeds(), super.getCollectedErrors(), super.getStatistics());
    }

    // Create visualization
    if (settings.isVisualization()) {
      try {
        Visualizer visualizer = new Visualizer(settings.getReportDirectory());
        visualizer.createVisualization(getDiscoveredSeeds());
      } catch (IOException e) {
        throw new CryptoAnalysisException("Couldn't create visualization: " + e.getMessage());
      }
    }
  }

  private CryptoAnalysisScope initializeFramework(DataFlowScope dataFlowScope) {
    FrameworkSetup frameworkSetup =
        new JimpleFrameworkSetup(
            settings.getApplicationPath(), settings.getCallGraph(), dataFlowScope);
    frameworkSetup.initializeFramework();
    super.getAnalysisReporter().beforeCallGraphConstruction();
    var frameworkScope = frameworkSetup.createFrameworkScope();
    super.getAnalysisReporter()
        .afterCallGraphConstruction(frameworkScope.asFrameworkScope().getCallGraph());
    return frameworkScope;
  }
}
