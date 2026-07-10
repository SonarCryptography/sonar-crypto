package org.sonarcrypto.ccerror.converters.ordererror;

import crypto.analysis.errors.TypestateError;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.FlowEntry;
import org.sonarcrypto.ccerror.causes.UnexpectedCallCause;
import org.sonarcrypto.ccerror.violations.CallViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils;
import org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils;
import org.sonarcrypto.utils.sonar.FqClassName;

@NullMarked
public class TypestateErrorConverter {
  private TypestateErrorConverter() {
    // Private constructor to prevent instantiation
  }

  public static Violation convert(TypestateError error) {

    final var unexpectedMethod = error.getErrorStatement().getInvokeExpr().getDeclaredMethod();
    final var expectedMethods = error.getExpectedMethodCalls();

    final var relevantStatements = error.getSeed().getRelevantStatements();

    final var flow =
        relevantStatements.keys().stream()
            .map(
                stmt -> {
                  final var position = ConverterUtils.intoPosition(stmt);

                  if (position == null) return null;

                  final var invokeExpr = stmt.getInvokeExpr();
                  final var message =
                      invokeExpr == null
                          ? "Relevant call"
                          : SignatureUtils.shortNameOf(
                              invokeExpr.getDeclaredMethod().getDeclaringClass(),
                              invokeExpr.getDeclaredMethod().getName());

                  return new FlowEntry(
                      new FqClassName(stmt.getMethod().getDeclaringClass().getFullyQualifiedName()),
                      position,
                      message);
                })
            .filter(Objects::nonNull)
            .toList();

    return new CallViolation(
        CryptoRulesDefinitions.API_MISUSE,
        new UnexpectedCallCause(unexpectedMethod, expectedMethods),
        flow);
  }
}
