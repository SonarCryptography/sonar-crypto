package org.sonarcrypto.ccerror.converters;

import static org.sonarcrypto.ccerror.converters.constrainterror.ConstraintErrorConverter.generateConstraintErrorMessage;

import crypto.analysis.errors.ImpreciseValueExtractionError;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.ccerror.violations.Violation;

@NullMarked
public class ImpreciseValueExtractionErrorConverter {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ImpreciseValueExtractionErrorConverter.class);

  public static @Nullable Violation convert(ImpreciseValueExtractionError error) {
    final var violatedConstraint = error.getViolatedConstraint();

    final var violatedConstraints =
        violatedConstraint.constraint().getViolatedConstraints().stream().findFirst().orElse(null);

    if (violatedConstraints == null) {
      LOGGER.error(
          "No violated constraints in {}! Generating general violation.",
          violatedConstraint.getClass().getName());
      return null;
    }

    return generateConstraintErrorMessage(violatedConstraints);
  }
}
