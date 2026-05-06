package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.PredicateContradictionError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.ArgViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.ccerror.violations.reasons.ShouldNotBeUsedHereReason;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class PredicateContradictionErrorConverter {
  public static Violation convert(PredicateContradictionError error) {
    final var contradictedPredicate = error.getContradictedPredicate();

    return new ArgViolation(
        CryptoRulesDefinitions.GENERAL,
        CallInfo.of(contradictedPredicate.statement(), contradictedPredicate.index()),
        new ShouldNotBeUsedHereReason());
  }
}
