package org.sonarcrypto.utils.cognicrypt.boomerang;

import boomerang.scope.Statement;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/** Information about a callee. */
@NullMarked
public record CalleeInfo(String className, String methodName, int argumentCount) {
  /**
   * Creates a callee info from the given statement. Returns {@code None}, if the statement does not
   * contain an invoke expression.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static Optional<CalleeInfo> of(Optional<Statement> statement) {
    return statement.flatMap(CalleeInfo::of);
  }

  /**
   * Creates a callee info from the given statement. Returns {@code None}, if the statement does not
   * contain an invoke expression.
   */
  public static Optional<CalleeInfo> of(Statement statement) {
    final var invokeExpr = statement.getInvokeExpr();

    if (invokeExpr == null) {
      return Optional.empty();
    }

    final var method = invokeExpr.getDeclaredMethod();

    return Optional.of(
        new CalleeInfo(
            method.getDeclaringClass().getFullyQualifiedName(),
            method.getName(),
            invokeExpr.getArgs().size()));
  }
}
