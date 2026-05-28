package org.sonarcrypto.ccerror;

import static org.sonarcrypto.utils.sonar.SonarFileSystemUtils.findInputFile;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.*;
import java.util.ArrayList;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.ccerror.causes.UndefinedCause;
import org.sonarcrypto.ccerror.converters.ForbiddenMethodErrorConverter;
import org.sonarcrypto.ccerror.converters.ImpreciseValueExtractionErrorConverter;
import org.sonarcrypto.ccerror.converters.PredicateContradictionErrorConverter;
import org.sonarcrypto.ccerror.converters.UncaughtExceptionErrorConverter;
import org.sonarcrypto.ccerror.converters.constrainterror.ConstraintErrorConverter;
import org.sonarcrypto.ccerror.converters.constrainterror.RequiredPredicateErrorConverter;
import org.sonarcrypto.ccerror.converters.ordererror.IncompleteOperationErrorConverter;
import org.sonarcrypto.ccerror.converters.ordererror.TypestateErrorConverter;
import org.sonarcrypto.ccerror.violations.CallViolation;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils;

@NullMarked
public class CcErrorConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(CcErrorConverter.class);

  private final FileSystem fileSystem;

  public CcErrorConverter(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  public FileSystem getFileSystem() {
    return this.fileSystem;
  }

  public ArrayList<ConvertedError> convertErrors(
      Table<WrappedClass, Method, Set<AbstractError>> issuesFromCC) {
    FileSystem fileSystem = this.fileSystem;

    final var violations = new ArrayList<ConvertedError>();

    for (Table.Cell<WrappedClass, Method, Set<AbstractError>> cell : issuesFromCC.cellSet()) {
      WrappedClass wrappedClass = cell.getRowKey();
      Method method = cell.getColumnKey();
      Set<AbstractError> errors = cell.getValue();

      // Find the InputFile corresponding to this class
      InputFile inputFile = findInputFile(fileSystem, wrappedClass);
      if (inputFile == null) {
        LOGGER.error(
            "Could not find source file for class: {}", wrappedClass.getFullyQualifiedName());
        continue;
      }

      final var overriddenErrors = new ArrayList<AbstractError>(errors.size());
      var atLeastOneErrorConverted = false;

      // Report each error in this class/method
      for (AbstractError error : errors) {
        if (!atLeastOneErrorConverted && !error.getPrecedingErrors().isEmpty()) {
          // Ignore preceding errors
          overriddenErrors.add(error);
          continue;
        }

        violations.add(
            new ConvertedError(
                inputFile,
                ConverterUtils.selectLocation(inputFile, error),
                method,
                convertError(error)));

        atLeastOneErrorConverted = true;
      }

      if (!atLeastOneErrorConverted) {
        // Report overridden errors if no other error was reported,
        // just in case that we do not miss any.
        for (final var error : overriddenErrors) {
          violations.add(
              new ConvertedError(
                  inputFile,
                  ConverterUtils.selectLocation(inputFile, error),
                  method,
                  convertError(error)));
        }
      }
    }

    return violations;
  }

  private Violation convertError(AbstractError error) {
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
      violation =
          new CallViolation(
              CryptoRulesDefinitions.GENERAL, new UndefinedCause(error.toErrorMarkerString()));
    }

    return violation;
  }
}
