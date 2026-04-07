package org.sonarcrypto.ccerror.converters.ordererror;

import static org.sonarcrypto.utils.sonar.TextUtils.code;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.IncompleteOperationError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.SimpleArgViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;
import org.sonarcrypto.utils.sonar.TextUtils;

@NullMarked
public class IncompleteOperationErrorConverter {
  public static Violation convert(IncompleteOperationError error) {

    final String incompleteObject;

    if (error.getSeed() instanceof AnalysisSeedWithSpecification seedWithSpec) {
      incompleteObject =
          String.format(" object of type %s", code(seedWithSpec.getSpecification().getClassName()));
    } else {
      incompleteObject = " object ";
    }

    final var expectedMethods = error.getExpectedMethodCalls();

    final var sb =
        new StringBuilder().append("Incomplete operation on ").append(incompleteObject).append('.');

    if (!expectedMethods.isEmpty()) {
      sb.append(" Expected call to either ")
          .append(
              TextUtils.join(expectedMethods.stream().map(it -> code(it.getMethodName())), "or"))
          .append(".");
    }

    return new SimpleArgViolation(
        CryptoRulesDefinitions.API_MISUSE, CallInfo.none(), sb.toString());
  }
}
