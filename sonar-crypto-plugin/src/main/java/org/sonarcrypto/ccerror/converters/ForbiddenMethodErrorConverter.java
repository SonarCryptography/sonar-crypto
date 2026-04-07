package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils.shortNameOf;
import static org.sonarcrypto.utils.sonar.TextUtils.code;

import crypto.analysis.errors.ForbiddenMethodError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.SimpleArgViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;
import org.sonarcrypto.utils.sonar.TextUtils;

@NullMarked
public class ForbiddenMethodErrorConverter {
  public static Violation convert(ForbiddenMethodError error) {

    final var forbiddenMethod = error.getCalledMethod();
    final var alternatives = error.getAlternatives();

    final var sb =
        new StringBuilder()
            .append("Call to the prohibited method ")
            .append(
                code(
                    shortNameOf(
                        forbiddenMethod.getDeclaringClass().getFullyQualifiedName(),
                        forbiddenMethod.getName())))
            .append('.');

    if (!alternatives.isEmpty()) {
      sb.append(" Consider calling one of ")
          .append(
              TextUtils.join(
                  alternatives.stream()
                      .map(it -> code(shortNameOf(it.getDeclaringClassName(), it.getMethodName()))),
                  "or"))
          .append(" instead.");
    }

    return new SimpleArgViolation(
        CryptoRulesDefinitions.FORBIDDEN_METHOD, CallInfo.none(), sb.toString());
  }
}
