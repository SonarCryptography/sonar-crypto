package org.sonarcrypto.ccerror.converters.constrainterror;

import crypto.analysis.errors.AbstractRequiredPredicateError;
import crypto.analysis.errors.AlternativeReqPredicateError;
import crypto.analysis.errors.RequiredPredicateError;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.ccerror.converters.RuleKindUtils;
import org.sonarcrypto.ccerror.violations.SimpleViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class RequiredPredicateErrorConverter {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(RequiredPredicateErrorConverter.class);

  public static Violation convert(AbstractRequiredPredicateError error) {
    if (error instanceof AlternativeReqPredicateError altReqPredError) {
      return generateAltReqPredMessage(altReqPredError);
    } else if (error instanceof RequiredPredicateError reqPredError) {
      return generateReqPredMessage(reqPredError);
    } else {
      LOGGER.error(
          "Unsupported required predicate error {}! Generating general violation.",
          error.getClass().getName());
      return SimpleViolation.general(CallInfo.none(), error.toErrorMarkerString());
    }
  }

  public static Violation generateAltReqPredMessage(AlternativeReqPredicateError error) {
    final var violatedPredicate = error.getViolatedPredicate();
    final var firstViolatedPredicate =
        violatedPredicate.predicates().stream().findFirst().orElse(null);

    if (firstViolatedPredicate == null) {
      LOGGER.error("Violated predicates are empty! Generating general violation.");
      return SimpleViolation.general(CallInfo.none(), error.toErrorMarkerString());
    }

    return SimpleViolation.of(
        RuleKindUtils.detectRuleKind(firstViolatedPredicate.predicate()),
        CallInfo.of(firstViolatedPredicate.statement(), firstViolatedPredicate.index()));
  }

  public static Violation generateReqPredMessage(RequiredPredicateError error) {
    final var contradictedPredicates = error.getContradictedPredicates();

    return SimpleViolation.general(
        CallInfo.of(contradictedPredicates.statement(), contradictedPredicates.index()));
  }
}
