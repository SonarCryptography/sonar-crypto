package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.ccerror.ConverterUtils.*;
import static org.sonarcrypto.utils.sonar.TextUtils.join;

import crypto.analysis.errors.ConstraintError;
import crypto.constraints.violations.ViolatedConstraint;
import crypto.constraints.violations.ViolatedValueConstraint;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

public class ConstraintErrorConverter {
  public static void convert(
      InputFile inputFile, NewIssueLocation location, ConstraintError error) {
    location
        .at(selectLocation(inputFile, error))
        .message(generateConstraintErrorMessage(error.getViolatedConstraint()));
  }

  public static String generateConstraintErrorMessage(ViolatedConstraint violatedConstraint) {
    if (violatedConstraint instanceof ViolatedValueConstraint violatedValueConstraint) {
      return generateViolatedValueConstraintMessage(violatedValueConstraint);
    }

    return violatedConstraint.getSimplifiedMessage(0);
  }

  public static String generateViolatedValueConstraintMessage(ViolatedValueConstraint constraint) {
    final var messageBuilder = new StringBuilder();

    final var violatingValues = constraint.violatingValues();
    final var violatingValuesCount = violatingValues.size();

    final var validValueRange = constraint.constraint().getConstraint().getValueRange();
    final var validValueRangeCount = validValueRange.size();

    final var calleeInfo = CalleeInfo.of(constraint.parameter().statement());

    messageBuilder.append(
        String.format(
            "The %s given to %s causes a cryptographic weakness.",
            stringifyArgumentIndex(
                constraint.parameter().index(),
                calleeInfo.map(CalleeInfo::parameterCount).orElse(null)),
            calleeInfo.map(CalleeInfo::name).orElse("the callee")));

    if (violatingValuesCount > 0) {
      final var violatedValuesStr =
          join(violatingValues.stream().map(it -> it.getTransformedVal().getStringValue()), "or");

      if (violatingValuesCount == 1) {
        messageBuilder.append(String.format("\nThe value %s is given", violatedValuesStr));
      } else {
        messageBuilder.append(String.format("\nThe values %s are given", violatedValuesStr));
      }
    } else {
      messageBuilder.append(validValueRangeCount > 0 ? "\nThe given value" : "");
    }

    if (validValueRangeCount > 0) {
      if (violatingValuesCount == 0) {
        messageBuilder.append("The value should be ");
      } else {
        messageBuilder.append(", but it should be ");
      }

      if (validValueRangeCount > 1) {
        if (violatingValuesCount > 1) {
          messageBuilder.append("contained in ").append(join(validValueRange, "and"));
        } else {
          messageBuilder.append("one of ").append(join(validValueRange, "or"));
        }
      }
    }

    messageBuilder.append('.');

    // System.out.println(messageBuilder);

    return messageBuilder.toString();
  }
}
