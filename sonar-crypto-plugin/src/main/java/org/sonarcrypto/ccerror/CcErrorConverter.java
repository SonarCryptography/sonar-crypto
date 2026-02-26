package org.sonarcrypto.ccerror;

import static org.sonarcrypto.ccerror.ConverterUtils.selectLocation;

import boomerang.scope.Method;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarcrypto.CryptoRulesDefinition;
import org.sonarcrypto.ccerror.converters.ConstraintErrorConverter;
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
                "Cryptographic weakness in method %s detected:\n\n",
                SignatureUtils.shortNameOf(method)));

    location.at(selectLocation(inputFile, error));

    if (error instanceof ConstraintError err) {
      issue.forRule(CryptoRulesDefinition.CC_RULE);
      ConstraintErrorConverter.convert(messageBuilder, err);
    }

    location.message(messageBuilder.toString());

    // System.out.println(error.toErrorMarkerString());
    // System.out.println();
    // System.out.println(messageBuilder);

    issue.at(location);
    issue.save();
  }
}
