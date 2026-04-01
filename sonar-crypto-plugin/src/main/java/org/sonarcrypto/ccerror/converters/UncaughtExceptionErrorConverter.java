package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.utils.sonar.TextUtils.code;

import crypto.analysis.errors.UncaughtExceptionError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.SimpleViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class UncaughtExceptionErrorConverter {
  public static Violation convert(UncaughtExceptionError error) {
    return new SimpleViolation(
        CryptoRulesDefinitions.UNCAUGHT_EXCEPTION,
        CallInfo.none(),
        String.format(
            "Uncaught exception %s.", code(error.getException().getFullyQualifiedName())));
  }
}
