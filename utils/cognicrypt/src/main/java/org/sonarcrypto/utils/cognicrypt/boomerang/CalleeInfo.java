package org.sonarcrypto.utils.cognicrypt.boomerang;

import boomerang.scope.Statement;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Information about a callee.
 *
 * @param name The callee name in a shortened form, bordered with backticks, e.g. <code>
 *     `MyClass.myMethod`</code>.
 * @param parameterCount The number of parameters.
 */
@NullMarked
public record CalleeInfo(String name, int parameterCount) {

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
