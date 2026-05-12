package org.sonarcrypto.ccerror;

import boomerang.scope.Method;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonarcrypto.ccerror.violations.Violation;

@NullMarked
public record ConvertedError(
    InputFile inputFile, TextRange position, Method method, Violation violation) {}
