package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.PredicateContradictionError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.ccerror.violations.SimpleArgViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class PredicateContradictionErrorConverter {
  public static Violation convert(PredicateContradictionError error) {
    final var contradictedPredicate = error.getContradictedPredicate();
    return SimpleArgViolation.general(
        CallInfo.of(contradictedPredicate.statement(), contradictedPredicate.index()),
        "should not be used here.");
  }
}
