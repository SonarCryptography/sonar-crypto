package org.sonarcrypto.ccerror;

import static java.lang.Math.max;

import boomerang.scope.Method;
import boomerang.scope.sootup.jimple.JimpleUpStatement;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.constraints.violations.ViolatedConstraint;
import crypto.constraints.violations.ViolatedValueConstraint;
import crypto.utils.CrySLUtils;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

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

    if (error instanceof ConstraintError err) convertConstraintError(inputFile, location, err);
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
      InputFile inputFile, NewIssueLocation location, ConstraintError error) {
    location
        .at(selectLocation(inputFile, error))
        .message(generateConstraintErrorMessage(error.getViolatedConstraint()));
  }

  private String generateConstraintErrorMessage(ViolatedConstraint violatedConstraint) {
    if (violatedConstraint instanceof ViolatedValueConstraint violatedValueConstraint) {

      final var messageBuilder = new StringBuilder();

      final var violatingValues = violatedValueConstraint.violatingValues();
      final var violatingValuesCount = violatingValues.size();

      final var validValueRange =
          violatedValueConstraint.constraint().getConstraint().getValueRange();
      final var validValueRangeCount = validValueRange.size();

      final var violatedValuesStr =
          join(violatingValues.stream().map(it -> it.getTransformedVal().getStringValue()), "or");
      final var validValuesStr = join(validValueRange, "or");

      final var calleeInfo = CalleeInfo.of(violatedValueConstraint.parameter().statement());

      messageBuilder.append(
          String.format(
              "The %s given to %s causes a cryptographic error.",
              stringifyArgumentIndex(
                  violatedValueConstraint.parameter().index(),
                  calleeInfo.map(CalleeInfo::parameterCount).orElse(null)),
              calleeInfo.map(CalleeInfo::name).orElse("the callee")));

      if (violatingValuesCount < 1)
        messageBuilder.append(validValueRangeCount > 0 ? "\nThe given value" : "");
      else if (violatingValuesCount == 1)
        messageBuilder.append(String.format("\nThe value %s is given", violatedValuesStr));
      else messageBuilder.append(String.format("\nThe values %s are given", violatedValuesStr));

      if (validValueRangeCount > 0) {
        if (violatingValuesCount == 0) messageBuilder.append("The value should be ");
        else messageBuilder.append(", but it should be ");

        if (validValueRangeCount > 1) messageBuilder.append("one of ");

        messageBuilder.append(validValuesStr);
      }

      messageBuilder.append('.');

      System.out.println(messageBuilder);

      return messageBuilder.toString();
    }

    return violatedConstraint.getSimplifiedMessage(0);
  }

  /**
   * Can't use {@link CrySLUtils#getIndexAsString}, because it returns "parameter" instead
   * "argument".
   */
  private String stringifyArgumentIndex(
      int zeroBasedArgumentIndex, @Nullable Integer parameterCount) {
    if (zeroBasedArgumentIndex < 0) return "return value";

    if (parameterCount != null && parameterCount == 1) return "argument";

    return switch (zeroBasedArgumentIndex) {
      case 0 -> "first argument";
      case 1 -> "second argument";
      case 2 -> "third argument";
      case 3 -> "fourth argument";
      case 4 -> "fifth argument";
      case 5 -> "sixth argument";
      default -> (zeroBasedArgumentIndex + 1) + "th argument";
    };
  }

  private static String join(Iterable<?> values, String lastSeparator) {
    return join(StreamSupport.stream(values.spliterator(), false), lastSeparator);
  }

  private static String join(Stream<?> values, @Nullable String lastSeparator) {
    final var iterator = values.iterator();

    if (!iterator.hasNext()) return "";

    final var sb = new StringBuilder();

    var valueCount = 0;

    while (iterator.hasNext()) {
      if (++valueCount > 1) sb.append(", ");

      final var next = iterator.next();

      if (valueCount > 2
          && lastSeparator != null
          && !lastSeparator.isEmpty()
          && !iterator.hasNext()) {
        sb.append(lastSeparator).append(' ');
      }

      if (next instanceof String s) sb.append(quote(s));
      else sb.append(next);
    }

    return sb.toString();
  }

  private static String quote(String value) {
    return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
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
