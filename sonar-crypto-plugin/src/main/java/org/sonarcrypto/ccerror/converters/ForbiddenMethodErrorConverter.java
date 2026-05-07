package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.ForbiddenMethodError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.causes.ForbiddenMethodCause;
import org.sonarcrypto.ccerror.violations.CallViolation;
import org.sonarcrypto.ccerror.violations.Violation;

@NullMarked
public class ForbiddenMethodErrorConverter {
  public static Violation convert(ForbiddenMethodError error) {

    return new CallViolation(
        CryptoRulesDefinitions.FORBIDDEN_METHOD,
        new ForbiddenMethodCause(error.getCalledMethod(), error.getAlternatives()));
  }
}
