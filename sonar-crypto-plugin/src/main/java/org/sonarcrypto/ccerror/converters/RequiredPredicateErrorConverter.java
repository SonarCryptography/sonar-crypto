package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.RequiredPredicateError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.SimpleViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class RequiredPredicateErrorConverter {
  public static Violation convert(RequiredPredicateError error) {
    final var contradictedPredicates = error.getContradictedPredicates();
    final var calleeInfo = CalleeInfo.of(contradictedPredicates.statement());

    return new SimpleViolation(
        CryptoRulesDefinitions.GENERAL,
        CallInfo.optOf(calleeInfo, contradictedPredicates.index()),
        "was cryptographically improper generated.");
  }
}
