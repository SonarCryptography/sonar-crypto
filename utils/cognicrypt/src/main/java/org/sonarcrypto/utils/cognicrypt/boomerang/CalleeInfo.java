package org.sonarcrypto.utils.cognicrypt.boomerang;

import boomerang.scope.Statement;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Information about a callee. */
@NullMarked
public record CalleeInfo(String className, String methodName, int argumentCount) {

  /**
   * Creates a callee info from the given statement. Returns {@code None}, if the statement does not
   * contain an invoke expression.
   */
  public static @Nullable CalleeInfo of(@Nullable Statement statement) {
    if (statement == null) {
      return null;
    }

    final var invokeExpr = statement.getInvokeExpr();

    if (invokeExpr == null) {
      return null;
    }

    final var method = invokeExpr.getDeclaredMethod();

    return new CalleeInfo(
        method.getDeclaringClass().getFullyQualifiedName(),
        method.getName(),
        invokeExpr.getArgs().size());
  }
}
