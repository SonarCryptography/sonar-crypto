package org.sonarcrypto.ccerror;

import boomerang.scope.Method;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.sonar.FqClassName;
import sootup.core.model.Position;

@NullMarked
public record ConvertedError(
    FqClassName className, Position position, Method method, Violation violation) {}
