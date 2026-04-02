package org.sonarcrypto.ccerror.converters.constrainterror;

import static java.lang.Math.min;
import static org.sonarcrypto.ccerror.RuleKindUtils.detectRuleKind;
import static org.sonarcrypto.utils.sonar.TextUtils.quote;

import crypto.analysis.errors.ConstraintError;
import crypto.constraints.violations.ViolatedBinaryConstraint;
import crypto.constraints.violations.ViolatedConstraint;
import crypto.constraints.violations.ViolatedNeverTypeOfConstraint;
import crypto.constraints.violations.ViolatedValueConstraint;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.*;
import org.sonarcrypto.utils.cognicrypt.crysl.Args;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class ConstraintErrorConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintErrorConverter.class);

  public static @Nullable Violation convert(ConstraintError error) {
    return generateConstraintErrorMessage(error.getViolatedConstraint());
  }

  public static @Nullable Violation generateConstraintErrorMessage(
      ViolatedConstraint violatedConstraint) {
    if (violatedConstraint instanceof ViolatedValueConstraint violatedValueConstraint) {
      return generateViolatedValueConstraintMessage(violatedValueConstraint);
    } else if (violatedConstraint
        instanceof ViolatedNeverTypeOfConstraint violatedNeverTypeOfConstraint) {
      return generateViolatedNeverTypeOfConstraintMessage(violatedNeverTypeOfConstraint);
    } else if (violatedConstraint instanceof ViolatedBinaryConstraint violatedBinaryConstraint) {
      return generateViolatedBinaryConstraintMessage(violatedBinaryConstraint);
    } else {
      LOGGER.error(
          "Unsupported required predicate error {}! Generating general violation.",
          violatedConstraint.getClass().getName());
      return null;
    }
  }

  static Violation generateViolatedValueConstraintMessage(ViolatedValueConstraint constraint) {
    final var violatingValues = constraint.violatingValues();
    final var valueConstraint = constraint.constraint();
    final var validValueRange = valueConstraint.getConstraint().getValueRange();

    final var MAX_SPLIT_LEN = 3;
    final var constraintVar = valueConstraint.getConstraint().getVar();
    final var splitter = constraintVar.getSplitter();

    return new ArgsViolation(
        detectRuleKind(constraintVar),
        CallInfo.of(constraint.parameter()),
        new Args(
            violatingValues.stream()
                .map(
                    violatingValue -> {
                      final var transformedVal = violatingValue.getTransformedVal();

                      if (!transformedVal.isStringConstant()) {
                        return transformedVal.toString();
                      }

                      final var stringValue = transformedVal.getStringValue();

                      if (splitter == null) {
                        return stringValue;
                      }

                      final var splitIndex = splitter.getIndex();

                      if (splitIndex < 0) {
                        return stringValue;
                      }

                      final var splitStringValue =
                          stringValue.split(splitter.getSplitter(), MAX_SPLIT_LEN);

                      return splitStringValue[min(splitIndex, MAX_SPLIT_LEN - 1)];
                    })
                .toList(),
            validValueRange));
  }

  static Violation generateViolatedNeverTypeOfConstraintMessage(
      ViolatedNeverTypeOfConstraint constraint) {
    return new SimpleViolation(
        CryptoRulesDefinitions.KEY_MATERIAL,
        CallInfo.of(constraint.parameter()),
        "should never be of the type " + quote(constraint.notAllowedType()) + ".");
  }

  static @Nullable Violation generateViolatedBinaryConstraintMessage(
      ViolatedBinaryConstraint constraint) {
    final var violatedConstraint =
        constraint.constraint().getRightConstraint().getViolatedConstraints().stream()
            .findFirst()
            .orElse(null);
    if (violatedConstraint == null) {
      LOGGER.error(
          "No violated constraints in {}! Generating general violation.",
          constraint.getClass().getName());
      return null;
    }

    return generateConstraintErrorMessage(violatedConstraint);
  }
}
