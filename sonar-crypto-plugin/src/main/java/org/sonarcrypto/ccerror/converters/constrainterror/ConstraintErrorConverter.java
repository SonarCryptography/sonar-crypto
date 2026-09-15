package org.sonarcrypto.ccerror.converters.constrainterror;

import static java.lang.Math.min;
import static org.sonarcrypto.ccerror.RuleKindUtils.detectRuleKind;

import crypto.analysis.errors.ConstraintError;
import crypto.constraints.violations.*;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.causes.*;
import org.sonarcrypto.ccerror.violations.ValueViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class ConstraintErrorConverter {

  private ConstraintErrorConverter() {
    // Private constructor to prevent instantiation
  }

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
    } else if (violatedConstraint
        instanceof ViolatedComparisonConstraint violatedComparisonConstraint) {
      return generateViolatedComparisonConstraintMessage(violatedComparisonConstraint);
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

    return new ValueViolation(
        detectRuleKind(constraintVar),
        CallInfo.of(constraint.parameter()),
        new InvalidEnumerableValueCause(
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

                      return splitStringValue[
                          min(splitIndex, min(MAX_SPLIT_LEN, splitStringValue.length) - 1)];
                    })
                .toList(),
            validValueRange),
        List.of(/* empty */ ));
  }

  static Violation generateViolatedNeverTypeOfConstraintMessage(
      ViolatedNeverTypeOfConstraint constraint) {
    return new ValueViolation(
        CryptoRulesDefinitions.KEY_MATERIAL,
        CallInfo.of(constraint.parameter()),
        new ForbiddenTypeCause(constraint.notAllowedType()),
        List.of(/* empty */ ));
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

  static Violation generateViolatedComparisonConstraintMessage(
      ViolatedComparisonConstraint constraint) {
    final var violatedConstraint = constraint.constraint().getConstraint();

    final var leftInvolvedNames = violatedConstraint.getLeft().getInvolvedVarNames();
    final var operator = violatedConstraint.getOperator();

    final Cause cause;

    if (leftInvolvedNames.contains("iterationCount")) {
      cause =
          new InvalidComparableValueCause(
              "iterationCount", operator, violatedConstraint.getRight().getLeft().getName());
    } else {
      // TODO: Stringify operands recursively without type info and irrelevant operands,
      //       so that, e.g., "int foo + int 0 > int 42 + int 0" becomes "foo > 42"
      cause =
          new InvalidComparableValueCause(
              violatedConstraint.getLeft().toString(),
              operator,
              violatedConstraint.getRight().toString());
    }

    return new ValueViolation(
        CryptoRulesDefinitions.KEY_MATERIAL,
        // Note: Cannot extract argument index, because it's a protected field
        //       (`IArithmeticConstraint.statementToValues`).
        CallInfo.of(constraint.statement(), -1),
        cause,
        List.of(/* empty */ ));
  }
}
