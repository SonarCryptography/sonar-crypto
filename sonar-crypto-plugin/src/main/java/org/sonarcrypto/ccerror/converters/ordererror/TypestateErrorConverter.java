package org.sonarcrypto.ccerror.converters.ordererror;

import crypto.analysis.errors.TypestateError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.causes.UnexpectedCallCause;
import org.sonarcrypto.ccerror.violations.CallViolation;
import org.sonarcrypto.ccerror.violations.Violation;

@NullMarked
public class TypestateErrorConverter {
  public static Violation convert(TypestateError error) {

    final var unexpectedMethod = error.getErrorStatement().getInvokeExpr().getDeclaredMethod();
    final var expectedMethods = error.getExpectedMethodCalls();

    return new CallViolation(
        CryptoRulesDefinitions.API_MISUSE,
        new UnexpectedCallCause(unexpectedMethod, expectedMethods));
  }
}
