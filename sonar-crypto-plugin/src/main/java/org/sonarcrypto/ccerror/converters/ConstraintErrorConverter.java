package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.ConstraintError;
import crypto.constraints.ValueConstraint;
import crypto.constraints.violations.ViolatedBinaryConstraint;
import crypto.constraints.violations.ViolatedConstraint;
import crypto.constraints.violations.ViolatedNeverTypeOfConstraint;
import crypto.constraints.violations.ViolatedValueConstraint;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.*;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;
import org.sonarcrypto.utils.cognicrypt.crysl.Args;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class ConstraintErrorConverter {
  public static @Nullable Violation convert(ConstraintError error) {
    return generateConstraintErrorMessage(error.getViolatedConstraint());
  }

  static @Nullable Violation generateConstraintErrorMessage(ViolatedConstraint violatedConstraint) {
    if (violatedConstraint instanceof ViolatedValueConstraint violatedValueConstraint) {
      return generateViolatedValueConstraintMessage(violatedValueConstraint);
    } else if (violatedConstraint
        instanceof ViolatedNeverTypeOfConstraint violatedNeverTypeOfConstraint) {
      return generateViolatedNeverTypeOfConstraintMessage(violatedNeverTypeOfConstraint);
    } else if (violatedConstraint instanceof ViolatedBinaryConstraint violatedBinaryConstraint) {
      return generateViolatedBinaryConstraintMessage(violatedBinaryConstraint);
    } else {
      return new SimpleViolation(
          CryptoRulesDefinitions.CC1_GENERAL,
          Optional.empty(),
          violatedConstraint.getSimplifiedMessage(0));
    }
  }

  static Violation generateViolatedValueConstraintMessage(ViolatedValueConstraint constraint) {
    final var violatingValues = constraint.violatingValues();
    final var validValueRange = constraint.constraint().getConstraint().getValueRange();
    final var calleeInfo = CalleeInfo.of(constraint.parameter().statement());

    return new ArgsViolation(
        CryptoRulesDefinitions.CC2_ALGORITHM,
        CallInfo.optOf(calleeInfo, constraint.parameter().index()),
        new Args(
            violatingValues.stream().map(it -> it.getTransformedVal().getStringValue()).toList(),
            validValueRange));
  }

  static Violation generateViolatedNeverTypeOfConstraintMessage(
      ViolatedNeverTypeOfConstraint constraint) {
    final var calleeInfo = CalleeInfo.of(constraint.parameter().statement());

    return new NeverTypeViolation(
        /* TODO: Use correct rule definition */ CryptoRulesDefinitions.CC1_GENERAL,
        CallInfo.optOf(calleeInfo, constraint.parameter().index()),
        constraint.notAllowedType());
  }

  static @Nullable Violation generateViolatedBinaryConstraintMessage(
      ViolatedBinaryConstraint constraint) {

    final var rightConstraint = constraint.constraint().getRightConstraint();

    if (rightConstraint instanceof ValueConstraint) {
      // TODO: Implement ...
    }

    return null;
  }
}
