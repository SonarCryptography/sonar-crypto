package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.ccerror.ConverterUtils.*;

import crypto.analysis.errors.RequiredPredicateError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

@NullMarked
public class RequiredPredicateErrorConverter {
  public static boolean convert(StringBuilder messageBuilder, RequiredPredicateError error) {
    final var contradictedPredicates = error.getContradictedPredicates();
    final var calleeInfo = CalleeInfo.of(contradictedPredicates.statement());

    messageBuilder
        .append(
            String.format(
                "The %s given to %s ",
                stringifyArgumentIndex(
                    contradictedPredicates.index(), calleeInfo.map(CalleeInfo::argumentCount)),
                stringifyCallee(calleeInfo)))
        .append("was cryptographically improper generated.");

    return true;
  }
}
