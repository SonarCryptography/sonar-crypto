package org.sonarcrypto;

import static org.sonarcrypto.utils.sonar.TextUtils.code;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.ccerror.ConvertedError;
import org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils;

/** Converts CogniCrypt (CryptoAnalysis) errors to SonarQube issues. */
@NullMarked
public class CcToSonarIssues {
  private static final Logger LOGGER = LoggerFactory.getLogger(CcToSonarIssues.class);

  /**
   * Reports all cryptographic errors found by CogniCrypt as SonarQube issues.
   *
   * @param context the SensorContext to create issues in
   * @param errors table of errors organized by class and method
   */
  public void reportAllIssues(SensorContext context, List<ConvertedError> errors) {

    for (final var entry : errors) {
      final var inputFile = entry.inputFile();
      final var position = entry.position();
      final var method = entry.method();
      final var violation = entry.violation();

      final var issue = context.newIssue();

      final var messageBuilder =
          new StringBuilder(
              String.format(
                  "Cryptographic weakness in method %s detected:\n",
                  code(SignatureUtils.shortNameOf(method))));

      final var location = issue.newLocation().on(inputFile);

      location.at(position);

      issue.forRule(violation.getRulesDefinition().getRuleKey());

      if (messageBuilder.length() > NewIssueLocation.MESSAGE_MAX_SIZE) {
        messageBuilder.setLength(NewIssueLocation.MESSAGE_MAX_SIZE);
      }

      violation.createMessage(messageBuilder);
      final var message = messageBuilder.toString();
      location.message(message);

      LOGGER.info(
          "{} @ [{}:{}/{}:{}]\n{}: {}",
          inputFile.filename(),
          position.start().line(),
          position.start().lineOffset(),
          position.end().line(),
          position.end().lineOffset(),
          violation.getRulesDefinition().getRuleKind(),
          violation.getCause());

      issue.at(location);
      issue.save();
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
    NewIssue issue = context.newIssue().forRule(CryptoRulesDefinitions.ALGORITHM.getRuleKey());
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
