package org.sonarcrypto.ccerror.converters.constrainterror;

import crypto.analysis.errors.AbstractRequiredPredicateError;
import crypto.analysis.errors.AlternativeReqPredicateError;
import crypto.analysis.errors.RequiredPredicateError;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.RuleKindUtils;
import org.sonarcrypto.ccerror.causes.ImproperGeneratedCause;
import org.sonarcrypto.ccerror.causes.UndefinedCause;
import org.sonarcrypto.ccerror.violations.ValueViolation;
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

      return new ValueViolation(
          CryptoRulesDefinitions.GENERAL,
          CallInfo.none(),
          new UndefinedCause(error.toErrorMarkerString()));
    }
  }

  public static Violation generateAltReqPredMessage(AlternativeReqPredicateError error) {
    final var violatedPredicate = error.getViolatedPredicate();
    final var firstViolatedPredicate =
        violatedPredicate.predicates().stream().findFirst().orElse(null);

    if (firstViolatedPredicate == null) {
      LOGGER.error("Violated predicates are empty! Generating general violation.");

      return new ValueViolation(
          CryptoRulesDefinitions.GENERAL,
          CallInfo.none(),
          new UndefinedCause(error.toErrorMarkerString()));
    }

    return new ValueViolation(
        RuleKindUtils.detectRuleKind(firstViolatedPredicate.predicate()),
        CallInfo.of(firstViolatedPredicate.statement(), firstViolatedPredicate.index()),
        new ImproperGeneratedCause());
  }

  public static Violation generateReqPredMessage(RequiredPredicateError error) {
    final var contradictedPredicates = error.getContradictedPredicates();

    return new ValueViolation(
        RuleKindUtils.detectRuleKind(contradictedPredicates.predicate()),
        CallInfo.of(contradictedPredicates.statement(), contradictedPredicates.index()),
        new ImproperGeneratedCause());
  }
}
