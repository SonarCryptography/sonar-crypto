package org.sonarcrypto;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.util.Iterator;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

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
        // TODO: Extract actual line number from error once API is confirmed
        // The AbstractError may have methods like getErrorLocation(), getStatement(), etc.
        // For now, report on line 1 as a placeholder
        int lineNumber = 1;
        String errorMessage =
            String.format(
                "Cryptographic error in method %s: %s",
                method.getName(), error.getClass().getSimpleName());
        reportIssue(context, inputFile, lineNumber, errorMessage);
      }
    }
  }

  /**
   * Finds the InputFile corresponding to a WrappedClass.
   *
   * @param fileSystem the file system to search in
   * @param wrappedClass the class to find the source file for
   * @return the InputFile, or null if not found
   */
  @Nullable
  public InputFile findInputFile(FileSystem fileSystem, WrappedClass wrappedClass) {
    String fullyQualifiedName = wrappedClass.getFullyQualifiedName();

    // Convert fully qualified class name to file path,
    // e.g., "com.example.MyClass" -> "com/example/MyClass.java"
    String relativePath = fullyQualifiedName.replace('.', '/') + ".java";

    FilePredicates predicates = fileSystem.predicates();
    Iterator<InputFile> files =
        fileSystem
            .inputFiles(
                predicates.and(
                    predicates.hasType(InputFile.Type.MAIN),
                    predicates.hasLanguage("java"),
                    predicates.matchesPathPattern("**/" + relativePath)))
            .iterator();

    return files.hasNext() ? files.next() : null;
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
