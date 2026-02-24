package org.sonarcrypto;

import static org.sonarcrypto.utils.sonar.SonarFileSystemUtils.findInputFile;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.ccerror.CcErrorConverter;

/** Converts CogniCrypt (CryptoAnalysis) errors to SonarQube issues. */
@NullMarked
public class CcToSonarIssues {
  private static final Logger LOGGER = LoggerFactory.getLogger(CcToSonarIssues.class);

  /**
   * Reports all cryptographic errors found by CogniCrypt as SonarQube issues.
   *
   * @param context the SensorContext to create issues in
   * @param issuesFromCC table of errors organized by class and method
   */
  public void reportAllIssues(
      SensorContext context, Table<WrappedClass, Method, Set<AbstractError>> issuesFromCC) {
    FileSystem fileSystem = context.fileSystem();

    for (Table.Cell<WrappedClass, Method, Set<AbstractError>> cell : issuesFromCC.cellSet()) {
      WrappedClass wrappedClass = cell.getRowKey();
      Method method = cell.getColumnKey();
      Set<AbstractError> errors = cell.getValue();

      // Find the InputFile corresponding to this class
      InputFile inputFile = findInputFile(fileSystem, wrappedClass);
      if (inputFile == null) {
        LOGGER.warn(
            "Could not find source file for class: {}", wrappedClass.getFullyQualifiedName());
        continue;
      }

      // Report each error in this class/method
      for (AbstractError error : errors) {
        new CcErrorConverter(context).convertError(inputFile, method, error);
        // reportIssue(context, inputFile, lineNumber, errorMessage);
      }
    }
  }

  /**
   * Reports a single cryptographic issue to SonarQube.
   *
   * @param context the SensorContext to create the issue in
   * @param inputFile the file containing the issue
   * @param line the line number where the issue occurs
   * @param errorMessage the error message to display
   */
  public void reportIssue(
      SensorContext context, InputFile inputFile, int line, String errorMessage) {
    NewIssue issue = context.newIssue().forRule(CryptoRulesDefinition.CC_RULE);
    NewIssueLocation location =
        issue
            .newLocation()
            .on(inputFile)
            .at(inputFile.selectLine(line))
            .message("Cryptographic API misuse: " + errorMessage);
    issue.at(location).save();

    LOGGER.debug("Reported issue in {} at line {}: {}", inputFile, line, errorMessage);
  }
}
