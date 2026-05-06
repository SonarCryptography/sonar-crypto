package org.sonarcrypto.ccerror.converters.constrainterror;

import crypto.analysis.errors.AbstractRequiredPredicateError;
import crypto.analysis.errors.AlternativeReqPredicateError;
import crypto.analysis.errors.RequiredPredicateError;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.RuleKindUtils;
import org.sonarcrypto.ccerror.violations.ArgViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.ccerror.violations.reasons.ImproperGeneratedReason;
import org.sonarcrypto.ccerror.violations.reasons.UndefinedReason;
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

      return new ArgViolation(
          CryptoRulesDefinitions.GENERAL,
          CallInfo.none(),
          new UndefinedReason(error.toErrorMarkerString()));
    }
  }

  public static Violation generateAltReqPredMessage(AlternativeReqPredicateError error) {
    final var violatedPredicate = error.getViolatedPredicate();
    final var firstViolatedPredicate =
        violatedPredicate.predicates().stream().findFirst().orElse(null);

    if (firstViolatedPredicate == null) {
      LOGGER.error("Violated predicates are empty! Generating general violation.");

      return new ArgViolation(
          CryptoRulesDefinitions.GENERAL,
          CallInfo.none(),
          new UndefinedReason(error.toErrorMarkerString()));
    }

    return new ArgViolation(
        RuleKindUtils.detectRuleKind(firstViolatedPredicate.predicate()),
        CallInfo.of(firstViolatedPredicate.statement(), firstViolatedPredicate.index()),
        new ImproperGeneratedReason());
  }

  public static Violation generateReqPredMessage(RequiredPredicateError error) {
    final var contradictedPredicates = error.getContradictedPredicates();

    return new ArgViolation(
        CryptoRulesDefinitions.GENERAL,
        CallInfo.of(contradictedPredicates.statement(), contradictedPredicates.index()),
        new ImproperGeneratedReason());
  }
}
