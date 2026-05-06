package org.sonarcrypto.ccerror.converters.ordererror;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.IncompleteOperationError;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.violations.CallViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.ccerror.violations.reasons.IncompleteOperationReason;

@NullMarked
public class IncompleteOperationErrorConverter {
  public static Violation convert(IncompleteOperationError error) {

    final IncompleteOperationReason.IncompleteObject incompleteObject =
        error.getSeed() instanceof AnalysisSeedWithSpecification seedWithSpec
            ? new IncompleteOperationReason.TypedIncompleteObject(
                seedWithSpec.getSpecification().getClassName())
            : new IncompleteOperationReason.UntypedIncompleteObject();

    final var expectedMethods = error.getExpectedMethodCalls();

    return new CallViolation(
        CryptoRulesDefinitions.API_MISUSE,
        new IncompleteOperationReason(incompleteObject, expectedMethods));
  }
}
