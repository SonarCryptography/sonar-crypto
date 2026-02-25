package org.sonarcrypto.utils.cognicrypt.boomerang;

import boomerang.scope.Statement;
import java.util.Optional;

public record CalleeInfo(String name, int parameterCount) {

  public static Optional<CalleeInfo> of(Statement statement) {
    final var invokeExpr = statement.getInvokeExpr();

    if (invokeExpr == null) {
      return Optional.empty();
    }

    final var method = invokeExpr.getDeclaredMethod();

    var className = method.getDeclaringClass().getFullyQualifiedName();
    final var lastDotIndex = className.lastIndexOf('.');

    if (lastDotIndex > 0) {
      className = className.substring(lastDotIndex + 1);
    }

    final var methodName = method.getName();

    return Optional.of(
        new CalleeInfo(
            "`" + className + "." + methodName + "`", method.getParameterTypes().size()));
  }
}
