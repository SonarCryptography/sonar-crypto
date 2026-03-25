package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.AlternativeReqPredicateError;
import crypto.constraints.RequiredPredicate;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.ccerror.violations.SimpleViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

@NullMarked
public class AlternativeReqPredicateErrorConverter {
  public static Violation convert(AlternativeReqPredicateError error) {
    final var violatedPredicate = error.getViolatedPredicate();
    final var firstViolatedPredicate = violatedPredicate.predicates().stream().findFirst();
    final var calleeInfo = CalleeInfo.of(firstViolatedPredicate.map(RequiredPredicate::statement));

    return SimpleViolation.general(
        calleeInfo, firstViolatedPredicate.map(RequiredPredicate::index));
  }
}
