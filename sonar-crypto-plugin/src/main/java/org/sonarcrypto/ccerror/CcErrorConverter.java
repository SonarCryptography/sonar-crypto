package org.sonarcrypto.ccerror;

import static java.lang.Math.max;

import boomerang.scope.Method;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonarcrypto.CryptoRulesDefinition;
import org.sonarcrypto.ccerror.converters.ConstraintErrorConverter;

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
    final var issue = this.getContext().newIssue().forRule(CryptoRulesDefinition.CC_RULE);
    final var location = issue.newLocation().on(inputFile);

    if (error instanceof ConstraintError err) {
      ConstraintErrorConverter.convert(inputFile, location, err);
    } else {
      location
          .at(inputFile.selectLine(max(error.getLineNumber(), 1)))
          .message(
              String.format(
                  "Cryptographic error in method %s: %s",
                  method.getName(), error.getClass().getSimpleName()));
    }

    issue.at(location);
    issue.save();
  }
}
