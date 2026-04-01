package org.sonarcrypto.ccerror.converters.ordererror;

import static org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils.shortNameOf;
import static org.sonarcrypto.utils.sonar.TextUtils.code;

import crypto.analysis.errors.TypestateError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.SimpleViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;
import org.sonarcrypto.utils.sonar.TextUtils;

@NullMarked
public class TypestateErrorConverter {
  public static Violation convert(TypestateError error) {

    final var unexpectedMethod = error.getErrorStatement().getInvokeExpr().getDeclaredMethod();
    final var expectedMethods = error.getExpectedMethodCalls();

    final var sb =
        new StringBuilder()
            .append("Unexpected call to method ")
            .append(
                code(
                    shortNameOf(
                        unexpectedMethod.getDeclaringClass().getFullyQualifiedName(),
                        unexpectedMethod.getName())))
            .append('.');

    if (!expectedMethods.isEmpty()) {
      sb.append(" Expected calling either ")
          .append(
              TextUtils.join(
                  expectedMethods.stream()
                      .map(it -> code(shortNameOf(it.getDeclaringClassName(), it.getMethodName()))),
                  "or"))
          .append(".");
    }

    return new SimpleViolation(CryptoRulesDefinitions.API_MISUSE, CallInfo.none(), sb.toString());
  }
}
