package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.AlternativeReqPredicateError;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.ccerror.violations.SimpleViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class AlternativeReqPredicateErrorConverter {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AlternativeReqPredicateErrorConverter.class);

  public static @Nullable Violation convert(AlternativeReqPredicateError error) {
    final var violatedPredicate = error.getViolatedPredicate();
    final var firstViolatedPredicate =
        violatedPredicate.predicates().stream().findFirst().orElse(null);

    if (firstViolatedPredicate == null) {
      LOGGER.error("Violated predicates are empty!");
      return null;
    }

    return SimpleViolation.of(
        RuleKindUtils.detectRuleKind(firstViolatedPredicate.predicate()),
        CallInfo.of(firstViolatedPredicate.statement(), firstViolatedPredicate.index()));
  }
}
