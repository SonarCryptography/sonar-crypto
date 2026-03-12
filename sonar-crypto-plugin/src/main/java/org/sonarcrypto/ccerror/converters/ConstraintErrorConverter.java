package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.ccerror.ConverterUtils.*;
import static org.sonarcrypto.utils.sonar.TextUtils.*;

import crypto.analysis.errors.ConstraintError;
import crypto.constraints.violations.ViolatedBinaryConstraint;
import crypto.constraints.violations.ViolatedConstraint;
import crypto.constraints.violations.ViolatedNeverTypeOfConstraint;
import crypto.constraints.violations.ViolatedValueConstraint;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

@NullMarked
public class ConstraintErrorConverter {
  public static @Nullable CryptoRulesDefinition convert(
      StringBuilder messageBuilder, ConstraintError error) {
    return generateConstraintErrorMessage(messageBuilder, error.getViolatedConstraint());
  }

  static @Nullable CryptoRulesDefinition generateConstraintErrorMessage(
      StringBuilder messageBuilder, ViolatedConstraint violatedConstraint) {
    if (violatedConstraint instanceof ViolatedValueConstraint violatedValueConstraint) {
      generateViolatedValueConstraintMessage(messageBuilder, violatedValueConstraint);
    } else if (violatedConstraint
        instanceof ViolatedNeverTypeOfConstraint violatedNeverTypeOfConstraint) {
      generateViolatedNeverTypeOfConstraintMessage(messageBuilder, violatedNeverTypeOfConstraint);
    } else if (violatedConstraint instanceof ViolatedBinaryConstraint violatedBinaryConstraint) {
      // TODO: generateViolatedBinaryConstraintMessage(messageBuilder, violatedBinaryConstraint);
      return null;
    } else {
      messageBuilder.append(violatedConstraint.getSimplifiedMessage(0));
    }

    return CryptoRulesDefinitions.CC1;
  }

  static void generateViolatedValueConstraintMessage(
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
                constraint.parameter().index(), calleeInfo.map(CalleeInfo::argumentCount)),
            stringifyCallee(calleeInfo)));

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

  static void generateViolatedNeverTypeOfConstraintMessage(
      StringBuilder messageBuilder, ViolatedNeverTypeOfConstraint constraint) {
    final var calleeInfo = CalleeInfo.of(constraint.parameter().statement());

    messageBuilder.append(
        String.format(
            "The %s given to %s ",
            stringifyArgumentIndex(
                constraint.parameter().index(), calleeInfo.map(CalleeInfo::argumentCount)),
            stringifyCallee(calleeInfo)));

    messageBuilder
        .append("should never be of the type ")
        .append(quote(constraint.notAllowedType()))
        .append('.');
  }

  static void generateViolatedBinaryConstraintMessage(
      StringBuilder messageBuilder, ViolatedBinaryConstraint constraint) {
    // TODO: Clarify, whether this is a composite error with errors nonetheless reported?

    // final var calleeInfo = CalleeInfo.of(constraint.parameter().statement());
    //
    // messageBuilder.append(
    //    String.format(
    //        "The %s given to %s ",
    //        stringifyArgumentIndex(
    //            constraint.parameter().index(),
    //            calleeInfo.map(CalleeInfo::argumentCount).orElse(null)),
    //        stringifyCallee(calleeInfo)));
    //
    // messageBuilder.append("should never be of the type
    // ").append(quote(constraint.notAllowedType())).append('.');

    // final var binaryConstraint = constraint.constraint();
    // final var leftConstraint = binaryConstraint.getLeftConstraint();
    // final var rightConstraint = binaryConstraint.getRightConstraint();
    //
    // final var errorMessage = constraint.getErrorMessage();
    // messageBuilder.append(errorMessage);
  }
}
