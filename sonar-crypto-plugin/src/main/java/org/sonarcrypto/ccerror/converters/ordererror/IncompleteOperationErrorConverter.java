package org.sonarcrypto.ccerror.converters.ordererror;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.IncompleteOperationError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.causes.IncompleteOperationCause;
import org.sonarcrypto.ccerror.violations.CallViolation;
import org.sonarcrypto.ccerror.violations.Violation;

@NullMarked
public class IncompleteOperationErrorConverter {
  public static Violation convert(IncompleteOperationError error) {

    final IncompleteOperationCause.IncompleteObject incompleteObject =
        error.getSeed() instanceof AnalysisSeedWithSpecification seedWithSpec
            ? new IncompleteOperationCause.TypedIncompleteObject(
                seedWithSpec.getSpecification().getClassName())
            : new IncompleteOperationCause.UntypedIncompleteObject();

    final var expectedMethods = error.getExpectedMethodCalls();

    return new CallViolation(
        CryptoRulesDefinitions.API_MISUSE,
        new IncompleteOperationCause(incompleteObject, expectedMethods));
  }
}
