package org.sonarcrypto.utils.cognicrypt.boomerang;

import boomerang.scope.Statement;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Information about a callee.
 *
 * @param name The callee name in a shortened form, bordered with backticks, e.g. <code>
 *     `MyClass.myMethod`</code>.
 * @param argumentCount The number of arguments.
 */
@NullMarked
public record CalleeInfo(String name, int argumentCount) {

  /**
   * Creates a callee info from the given statement. Returns {@code None}, if the statement does not
   * contain an invoke expression.
   */
  public static Optional<CalleeInfo> of(Statement statement) {
    final var invokeExpr = statement.getInvokeExpr();

    return invokeExpr == null
        ? Optional.empty()
        : Optional.of(
            new CalleeInfo(
                SignatureUtils.shortNameOf(invokeExpr.getDeclaredMethod()),
                invokeExpr.getArgs().size()));
  }
}
