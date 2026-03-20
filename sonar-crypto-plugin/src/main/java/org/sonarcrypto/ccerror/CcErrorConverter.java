package org.sonarcrypto.ccerror;

import static org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils.selectLocation;

import boomerang.scope.Method;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.AlternativeReqPredicateError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.RequiredPredicateError;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.converters.AlternativeReqPredicateErrorConverter;
import org.sonarcrypto.ccerror.converters.ConstraintErrorConverter;
import org.sonarcrypto.ccerror.converters.RequiredPredicateErrorConverter;
import org.sonarcrypto.ccerror.violations.SimpleViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils;

@NullMarked
public class CcErrorConverter {
  private final SensorContext context;

  public CcErrorConverter(SensorContext context) {
    this.context = context;
  }

  public SensorContext getContext() {
    return this.context;
  }

  public void convertError(InputFile inputFile, Method method, AbstractError error) {
    final Violation violation;

    if (error instanceof AlternativeReqPredicateError err) {
      violation = AlternativeReqPredicateErrorConverter.convert(err);
    } else if (error instanceof ConstraintError err) {
      violation = ConstraintErrorConverter.convert(err);
    } else if (error instanceof RequiredPredicateError err) {
      violation = RequiredPredicateErrorConverter.convert(err);
    } else {
      violation =
          new SimpleViolation(
              CryptoRulesDefinitions.CC1_GENERAL, Optional.empty(), error.toErrorMarkerString());
    }

    if (violation == null) return;

    final var issue = this.getContext().newIssue();
    final var location = issue.newLocation().on(inputFile);

    final var messageBuilder =
        new StringBuilder(
            String.format(
                "Cryptographic weakness in method %s detected:\n",
                SignatureUtils.shortNameOf(method)));

    location.at(selectLocation(inputFile, error));

    issue.forRule(violation.rulesDefinition().getRuleKey());

    if (messageBuilder.length() > NewIssueLocation.MESSAGE_MAX_SIZE) {
      messageBuilder.setLength(NewIssueLocation.MESSAGE_MAX_SIZE);
    }

    violation.createMessage(messageBuilder);
    location.message(messageBuilder.toString());

    // System.out.println(error.toErrorMarkerString());
    // System.out.println();
    // System.out.println(messageBuilder);
    // System.out.println();

    issue.at(location);
    issue.save();
  }
}
