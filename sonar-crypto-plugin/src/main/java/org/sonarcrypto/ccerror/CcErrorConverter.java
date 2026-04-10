package org.sonarcrypto.ccerror;

import static org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils.selectLocation;
import static org.sonarcrypto.utils.sonar.TextUtils.code;

import boomerang.scope.Method;
import crypto.analysis.errors.*;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.ccerror.converters.ForbiddenMethodErrorConverter;
import org.sonarcrypto.ccerror.converters.ImpreciseValueExtractionErrorConverter;
import org.sonarcrypto.ccerror.converters.PredicateContradictionErrorConverter;
import org.sonarcrypto.ccerror.converters.UncaughtExceptionErrorConverter;
import org.sonarcrypto.ccerror.converters.constrainterror.ConstraintErrorConverter;
import org.sonarcrypto.ccerror.converters.constrainterror.RequiredPredicateErrorConverter;
import org.sonarcrypto.ccerror.converters.ordererror.IncompleteOperationErrorConverter;
import org.sonarcrypto.ccerror.converters.ordererror.TypestateErrorConverter;
import org.sonarcrypto.ccerror.violations.SimpleArgViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public class CcErrorConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(CcErrorConverter.class);

  private final SensorContext context;

  public CcErrorConverter(SensorContext context) {
    this.context = context;
  }

  public SensorContext getContext() {
    return this.context;
  }

  public boolean convertError(InputFile inputFile, Method method, AbstractError error) {
    Violation violation = null;

    if (error instanceof AbstractRequiredPredicateError err) {
      violation = RequiredPredicateErrorConverter.convert(err);
    } else if (error instanceof ConstraintError err) {
      violation = ConstraintErrorConverter.convert(err);
    } else if (error instanceof ForbiddenMethodError err) {
      violation = ForbiddenMethodErrorConverter.convert(err);
    } else if (error instanceof UncaughtExceptionError err) {
      violation = UncaughtExceptionErrorConverter.convert(err);
    } else if (error instanceof TypestateError err) {
      violation = TypestateErrorConverter.convert(err);
    } else if (error instanceof IncompleteOperationError err) {
      violation = IncompleteOperationErrorConverter.convert(err);
    } else if (error instanceof ImpreciseValueExtractionError err) {
      violation = ImpreciseValueExtractionErrorConverter.convert(err);
    } else if (error instanceof PredicateContradictionError err) {
      violation = PredicateContradictionErrorConverter.convert(err);
    }

    if (violation == null) {
      violation = SimpleArgViolation.general(CallInfo.none(), error.toErrorMarkerString());
    }

    final var issue = this.getContext().newIssue();
    final var location = issue.newLocation().on(inputFile);

    final var messageBuilder =
        new StringBuilder(
            String.format(
                "Cryptographic weakness in method %s detected:\n",
                code(SignatureUtils.shortNameOf(method))));

    location.at(selectLocation(inputFile, error));

    issue.forRule(violation.rulesDefinition().getRuleKey());

    if (messageBuilder.length() > NewIssueLocation.MESSAGE_MAX_SIZE) {
      messageBuilder.setLength(NewIssueLocation.MESSAGE_MAX_SIZE);
    }

    violation.createMessage(messageBuilder);
    final var message = messageBuilder.toString();
    location.message(message);

    LOGGER.info("{}: {}", violation.rulesDefinition().getRuleKind(), message);

    issue.at(location);
    issue.save();

    return true;
  }
}
