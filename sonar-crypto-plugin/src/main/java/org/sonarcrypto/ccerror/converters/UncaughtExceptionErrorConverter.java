package org.sonarcrypto.ccerror.converters;

import crypto.analysis.errors.UncaughtExceptionError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.causes.UncaughtExceptionCause;
import org.sonarcrypto.ccerror.violations.CallViolation;
import org.sonarcrypto.ccerror.violations.Violation;

@NullMarked
public class UncaughtExceptionErrorConverter {
  public static Violation convert(UncaughtExceptionError error) {
    return new CallViolation(
        CryptoRulesDefinitions.UNCAUGHT_EXCEPTION,
        new UncaughtExceptionCause(error.getException()));
  }
}
