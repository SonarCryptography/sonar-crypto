package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.ccerror.ConverterUtils.*;
import static org.sonarcrypto.utils.sonar.TextUtils.join;

import crypto.analysis.errors.ConstraintError;
import crypto.constraints.violations.ViolatedConstraint;
import crypto.constraints.violations.ViolatedValueConstraint;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

public class ConstraintErrorConverter {
  public static void convert(StringBuilder messageBuilder, ConstraintError error) {
    generateConstraintErrorMessage(messageBuilder, error.getViolatedConstraint());
  }

  public static void generateConstraintErrorMessage(
      StringBuilder messageBuilder, ViolatedConstraint violatedConstraint) {
    if (violatedConstraint instanceof ViolatedValueConstraint violatedValueConstraint) {
      generateViolatedValueConstraintMessage(messageBuilder, violatedValueConstraint);
      // generateViolatedValueConstraintMessageShort(messageBuilder, violatedValueConstraint);
    } else {
      messageBuilder.append(violatedConstraint.getSimplifiedMessage(0));
    }
  }

  public static void generateViolatedValueConstraintMessage(
      StringBuilder messageBuilder, ViolatedValueConstraint constraint) {
    final var violatingValues = constraint.violatingValues();
    final var violatingValuesCount = violatingValues.size();

    final var validValueRange = constraint.constraint().getConstraint().getValueRange();
    final var validValueRangeCount = validValueRange.size();

    final var calleeInfo = CalleeInfo.of(constraint.parameter().statement());

    messageBuilder.append(
        String.format(
            "The %s given to %s ",
            stringifyArgumentIndex(
                constraint.parameter().index(),
                calleeInfo.map(CalleeInfo::argumentCount).orElse(null)),
            calleeInfo.map(CalleeInfo::name).orElse("the callee")));

    if (violatingValuesCount > 0) {
      if (violatingValuesCount == 1) {
        messageBuilder.append("has the value ");
      } else {
        messageBuilder.append("has the values ");
      }

      messageBuilder.append(
          join(
              violatingValues.stream().map(it -> it.getTransformedVal().getStringValue()),
              "or",
              "respectively"));
    } else {
      messageBuilder.append(validValueRangeCount > 0 ? "\nThe given value" : "");
    }

    if (validValueRangeCount > 0) {
      if (violatingValuesCount == 0) {
        messageBuilder.append("should be ");
      }
      if (violatingValuesCount == 1) {
        messageBuilder.append(", but it should be ");
      }
    } else {
      messageBuilder.append(", but they should be ");
    }

    if (validValueRangeCount > 1) {
      if (violatingValuesCount > 1) {
        messageBuilder.append("contained in ").append(join(validValueRange, "and"));
      } else {
        messageBuilder.append("one of ").append(join(validValueRange, "or"));
      }
    }

    messageBuilder.append('.');
  }

  // public static void generateViolatedValueConstraintMessageShort(
  //	StringBuilder messageBuilder,
  //	ViolatedValueConstraint constraint
  // ) {
  //	final var violatingValues = constraint.violatingValues();
  //	final var validValueRange = constraint.constraint().getConstraint().getValueRange();
  //	final var calleeInfo = CalleeInfo.of(constraint.parameter().statement());
  //
  //	messageBuilder.append(
  //		String.format(
  //			"The %s given to %s contains invalid value(s).\nGiven value(s): %s\nValid value(s): %s",
  //			stringifyArgumentIndex(
  //				constraint.parameter().index(),
  //				calleeInfo.map(CalleeInfo::argumentCount).orElse(null)
  //			),
  //			calleeInfo.map(CalleeInfo::name).orElse("the callee"),
  //			join(
  //				violatingValues.stream().map(it -> it.getTransformedVal().getStringValue()),
  //				"or",
  //				"respectively"
  //			),
  //			join(validValueRange, "or")
  //		)
  //	);
  // }
}
