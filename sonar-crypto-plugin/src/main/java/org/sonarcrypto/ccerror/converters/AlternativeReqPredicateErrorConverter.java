package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.ccerror.ConverterUtils.*;

import crypto.analysis.errors.AlternativeReqPredicateError;
import crypto.constraints.RequiredPredicate;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

@NullMarked
public class AlternativeReqPredicateErrorConverter {
  public static CryptoRulesDefinition convert(
      StringBuilder messageBuilder, AlternativeReqPredicateError error) {
    final var violatedPredicate = error.getViolatedPredicate();
    final var firstViolatedPredicate = violatedPredicate.predicates().stream().findFirst();
    final var calleeInfo = CalleeInfo.of(firstViolatedPredicate.map(RequiredPredicate::statement));

    messageBuilder
        .append(
            String.format(
                "The %s given to %s ",
                stringifyArgumentIndex(
                    firstViolatedPredicate.map(RequiredPredicate::index).orElse(-1),
                    calleeInfo.map(CalleeInfo::argumentCount)),
                stringifyCallee(calleeInfo)))
        .append("was cryptographically improper generated.");

    return CryptoRulesDefinitions.CC2_UA;
  }
}
