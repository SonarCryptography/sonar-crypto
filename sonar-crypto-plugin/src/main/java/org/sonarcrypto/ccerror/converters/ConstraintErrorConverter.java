package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.ccerror.converters.RuleKindUtils.detectRuleKind;
import static org.sonarcrypto.utils.sonar.TextUtils.quote;

import crypto.analysis.errors.ConstraintError;
import crypto.constraints.ValueConstraint;
import crypto.constraints.violations.ViolatedBinaryConstraint;
import crypto.constraints.violations.ViolatedConstraint;
import crypto.constraints.violations.ViolatedNeverTypeOfConstraint;
import crypto.constraints.violations.ViolatedValueConstraint;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.*;
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
      return SimpleViolation.general(CallInfo.none(), violatedConstraint.getSimplifiedMessage(0));
    }
  }

  static Violation generateViolatedValueConstraintMessage(ViolatedValueConstraint constraint) {
    final var violatingValues = constraint.violatingValues();
    final var valueConstraint = constraint.constraint();
    final var validValueRange = valueConstraint.getConstraint().getValueRange();

    return new ArgsViolation(
        detectRuleKind(valueConstraint.getConstraint().getVar()),
        CallInfo.of(constraint.parameter()),
        new Args(
            violatingValues.stream().map(it -> it.getTransformedVal().getStringValue()).toList(),
            validValueRange));
  }

  static Violation generateViolatedNeverTypeOfConstraintMessage(
      ViolatedNeverTypeOfConstraint constraint) {
    return new SimpleViolation(
        CryptoRulesDefinitions.FORBIDDEN_TYPE,
        CallInfo.of(constraint.parameter()),
        "should never be of the type " + quote(constraint.notAllowedType()) + ".");
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
