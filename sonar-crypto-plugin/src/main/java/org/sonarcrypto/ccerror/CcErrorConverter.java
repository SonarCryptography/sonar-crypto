package org.sonarcrypto.ccerror;

import static java.lang.Math.max;

import boomerang.scope.Method;
import boomerang.scope.sootup.jimple.JimpleUpStatement;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.constraints.violations.ViolatedConstraint;
import crypto.constraints.violations.ViolatedValueConstraint;
import crypto.utils.CrySLUtils;
import java.util.Locale;
import java.util.StringJoiner;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.CryptoRulesDefinition;

@NullMarked
public class CcErrorConverter {
  private final SensorContext context;

  public CcErrorConverter(SensorContext context) {
    this.context = context;
  }

  public SensorContext getContext() {
    return this.context;
  }

  public void convertError(InputFile inputFile, Method method, AbstractError error) {
    final var issue = this.getContext().newIssue().forRule(CryptoRulesDefinition.CC_RULE);
    final var location = issue.newLocation().on(inputFile);

    if (error instanceof ConstraintError err)
      convertConstraintError(inputFile, location, method, err);
    else {
      location
          .at(inputFile.selectLine(max(error.getLineNumber(), 1)))
          .message(
              String.format(
                  "Cryptographic error in method %s: %s",
                  method.getName(), error.getClass().getSimpleName()));
    }

    issue.at(location);
    issue.save();
  }

  private void convertConstraintError(
      InputFile inputFile, NewIssueLocation location, Method method, ConstraintError error) {
    location
        .at(selectLocation(inputFile, error))
        .message(generateConstraintErrorMessage(error.getViolatedConstraint()));
  }

  private String generateConstraintErrorMessage(ViolatedConstraint violatedConstraint) {
    if (violatedConstraint instanceof ViolatedValueConstraint vc) {
      final var nth = CrySLUtils.getIndexAsString(vc.parameter().index()).toLowerCase(Locale.ROOT);
      final var violatingValues = vc.violatingValues();

      final var messageBuilder =
          new StringBuilder()
              .append("The ")
              .append(nth)
              .append(" given to the callee causes a cryptographic error.\n");

      final var violatingValuesIterator = violatingValues.iterator();

      final var validValueRange = vc.constraint().getConstraint().getValueRange();
      final var validValueRangeCount = validValueRange.size();

      if (!violatingValuesIterator.hasNext()) {
        if (validValueRangeCount > 0) messageBuilder.append("Its value should be ");
      } else {
        final var joiner = new StringJoiner(", ");
        joiner.add(violatingValuesIterator.next().getTransformedVal().getStringValue());

        if (!violatingValuesIterator.hasNext()) {
          messageBuilder.append("The value ").append(joiner).append(" is ");
        } else {
          messageBuilder.append("The values ");

          do {
            joiner.add(violatingValuesIterator.next().getTransformedVal().getStringValue());
          } while (violatingValuesIterator.hasNext());

          messageBuilder.append(" are ").append(joiner);
        }

        messageBuilder.append("given");

        if (validValueRangeCount > 0) messageBuilder.append(", but it should be ");
      }

      if (validValueRangeCount > 0) {

        if (validValueRangeCount > 1) messageBuilder.append("one of ");

        final var joiner = new StringJoiner(", ");
        validValueRange.forEach(joiner::add);
        messageBuilder.append(joiner);
      }

      messageBuilder.append('.');

      // System.out.println(messageBuilder);

      return messageBuilder.toString();
    }

    return violatedConstraint.getSimplifiedMessage(0);
  }

  private TextRange selectLocation(InputFile inputFile, AbstractError error) {
    final var stmt = error.getErrorStatement();

    if (stmt instanceof JimpleUpStatement upStmt) {
      final var positionInfo = upStmt.getDelegate().getPositionInfo();

      final var position = positionInfo.getStmtPosition();

      final var startLine = position.getFirstLine();
      final var startLineOffset = position.getFirstCol();
      final var endLine = position.getLastLine();
      final var endLineOffset = position.getLastCol();

      if (startLine < 1) {
        return inputFile.selectLine(1);
      }

      if (endLineOffset < 1) {
        return inputFile.selectLine(startLine);
      }

      return inputFile.newRange(
          startLine, startLineOffset, max(endLine - 1, startLine), endLineOffset);
    }

    return inputFile.selectLine(error.getLineNumber());
  }
}
