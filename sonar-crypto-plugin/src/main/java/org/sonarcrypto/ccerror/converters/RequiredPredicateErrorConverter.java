package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.RequiredPredicateError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.ccerror.violations.SimpleViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class RequiredPredicateErrorConverter {
  public static Violation convert(RequiredPredicateError error) {
    final var contradictedPredicates = error.getContradictedPredicates();

    return SimpleViolation.general(
        CallInfo.of(contradictedPredicates.statement(), contradictedPredicates.index()));
  }
}
