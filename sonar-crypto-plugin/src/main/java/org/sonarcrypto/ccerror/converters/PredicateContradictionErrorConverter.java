package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.PredicateContradictionError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.causes.ShouldNotBeUsedHereCause;
import org.sonarcrypto.ccerror.violations.ValueViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class PredicateContradictionErrorConverter {
  public static Violation convert(PredicateContradictionError error) {
    final var contradictedPredicate = error.getContradictedPredicate();

    return new ValueViolation(
        CryptoRulesDefinitions.GENERAL,
        CallInfo.of(contradictedPredicate.statement(), contradictedPredicate.index()),
        new ShouldNotBeUsedHereCause());
  }
}
