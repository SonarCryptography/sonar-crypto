package org.sonarcrypto;

import static org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils.shortNameOf;
import static org.sonarcrypto.utils.sonar.SonarFileSystemUtils.findInputFile;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.ccerror.ConvertedError;
import org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

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

      // Find the InputFile corresponding to this class
      InputFile inputFile = findInputFile(context.fileSystem(), entry.className());

      if (inputFile == null) {
        LOGGER.error("Could not find source file for class: {}", entry.className().fqn());
        continue;
      }

      final var position = ConverterUtils.selectLocation(inputFile, entry.position());
      final var method = entry.method();
      final var violation = entry.violation();

      final var issue = context.newIssue();

      final var messageBuilder =
          new MessageCrafter()
              .text("Cryptographic weakness in method ")
              .code(shortNameOf(method))
              .text(" detected:")
              .newLine();

      final var location = issue.newLocation().on(inputFile);

      try {
        location.at(position);
      } catch (IllegalArgumentException e) {
        LOGGER.error("Invalid source code position in file {}!", inputFile, e);
      }

      issue.forRule(violation.getRulesDefinition().getRuleKey());

      violation.createMessage(messageBuilder);

      messageBuilder.addMessageTo(location);

      final var flow = violation.getFlow();

      if (!flow.isEmpty()) {
        final var flowLocations = new ArrayList<NewIssueLocation>(flow.size());

        for (final var flowEntry : flow) {
          InputFile flowFile = findInputFile(context.fileSystem(), flowEntry.className());

          if (flowFile == null) {
            LOGGER.error(
                "Could not find source file for execution flow class: {}",
                flowEntry.className().fqn());
            continue;
          }

          final var flowLocation = issue.newLocation().on(flowFile);
          final var flowPosition = ConverterUtils.selectLocation(flowFile, flowEntry.position());

          flowEntry.message().addMessageTo(flowLocation);
          flowLocation.at(flowPosition);
          flowLocations.add(flowLocation);
        }

        issue.addFlow(flowLocations, NewIssue.FlowType.EXECUTION, null);
      }

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
            "{} @ [{}:{}/{}:{}] {}: {}",
            inputFile.filename(),
            position.start().line(),
            position.start().lineOffset(),
            position.end().line(),
            position.end().lineOffset(),
            violation.getRulesDefinition().getRuleKind(),
            violation.getCause());
      }

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
