package org.sonarcrypto.ccerror;

import static org.sonarcrypto.ccerror.ConverterUtils.selectLocation;

import boomerang.scope.Method;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.AlternativeReqPredicateError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.RequiredPredicateError;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.converters.AlternativeReqPredicateErrorConverter;
import org.sonarcrypto.ccerror.converters.ConstraintErrorConverter;
import org.sonarcrypto.ccerror.converters.RequiredPredicateErrorConverter;
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
    final var issue = this.getContext().newIssue();
    final var location = issue.newLocation().on(inputFile);

    final var messageBuilder =
        new StringBuilder(
            String.format(
                "Cryptographic weakness in method %s detected:\n",
                SignatureUtils.shortNameOf(method)));

    issue.forRule(CryptoRulesDefinitions.CC1.getRuleKey());

    location.at(selectLocation(inputFile, error));

    final boolean converted;

    if (error instanceof AlternativeReqPredicateError err) {
      converted = AlternativeReqPredicateErrorConverter.convert(messageBuilder, err);
    } else if (error instanceof ConstraintError err) {
      converted = ConstraintErrorConverter.convert(messageBuilder, err);
    } else if (error instanceof RequiredPredicateError err) {
      converted = RequiredPredicateErrorConverter.convert(messageBuilder, err);
    } else {
      messageBuilder.append(error.toErrorMarkerString());
      converted = true;
    }

    if (!converted) return;

    if (messageBuilder.length() > NewIssueLocation.MESSAGE_MAX_SIZE) {
      messageBuilder.setLength(NewIssueLocation.MESSAGE_MAX_SIZE);
    }

    location.message(messageBuilder.toString());

    // System.out.println(error.toErrorMarkerString());
    // System.out.println();
    // System.out.println(messageBuilder);
    // System.out.println();

    issue.at(location);
    issue.save();
  }
}
