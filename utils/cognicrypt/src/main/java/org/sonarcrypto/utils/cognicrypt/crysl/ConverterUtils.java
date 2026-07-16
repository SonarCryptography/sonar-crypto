package org.sonarcrypto.utils.cognicrypt.crysl;

import static java.lang.Math.*;
import static org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils.shortNameOf;

import boomerang.scope.Statement;
import boomerang.scope.sootup.jimple.JimpleUpStatement;
import crypto.analysis.errors.AbstractError;
import crypto.utils.CrySLUtils;
import crysl.rule.CrySLMethod;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;
import sootup.core.model.LinePosition;
import sootup.core.model.Position;

public class ConverterUtils {

  private ConverterUtils() {
    // Utility class
  }

  /**
   * Stringifies the callee from a callee info, e.g., {@code `Foo.bar`}. Constructor and static
   * constructor are handled differently, e.g., {@code `Foo`'s constructor}. If the callee info is
   * {@code null}, the string {@code the callee} is returned.
   */
  public static void stringifyCallee(
      MessageCrafter messageCrafter, @Nullable CalleeInfo calleeInfo) {
    if (calleeInfo == null) {
      messageCrafter.text("the callee");
      return;
    }

    switch (calleeInfo.methodName()) {
      case "<init>" ->
          messageCrafter.code(shortNameOf(calleeInfo.className())).text("'s constructor");
      case "<clinit>" ->
          messageCrafter.code(shortNameOf(calleeInfo.className())).text("'s static constructor");
      default -> messageCrafter.code(shortNameOf(calleeInfo.className(), calleeInfo.methodName()));
    }
  }

  /**
   * Converts a zero-based argument index into an ordinal form.
   *
   * <table>
   *     <tr>
   *         <th>Index</th>
   *         <th>Result</th>
   *     </tr>
   *     <tr>
   *         <td><code>&lt; 0 || argumentCount == 1</code></td>
   *         <td>just <code>"argument"</code></td>
   *     </tr>
   *     <tr>
   *         <td><code>&gt; 0 && &lt; 6</code></td>
   *         <td>ordinal as a word, e.g., <code>"third argument"</code></td>
   *     </tr>
   *     <tr>
   *         <td><i>else</i></td>
   *         <td>ordinal as a number, e.g., <code>"7th argument"</code></td>
   *     </tr>
   * </table>
   *
   * <p>See also {@link CrySLUtils#getIndexAsString} for parameters that also considers a negative
   * index as "return value".
   */
  public static String stringifyArgumentIndex(int zeroBasedArgumentIndex, int parameterCount) {
    if (zeroBasedArgumentIndex < 0 || parameterCount == 1) return "argument";

    return switch (zeroBasedArgumentIndex) {
      case 0 -> "first argument";
      case 1 -> "second argument";
      case 2 -> "third argument";
      case 3 -> "fourth argument";
      case 4 -> "fifth argument";
      case 5 -> "sixth argument";
      default -> (zeroBasedArgumentIndex + 1) + "th argument";
    };
  }

  /**
   * Selects a location as precise as possible, if the {@link AbstractError#getErrorStatement()
   * error's statement} is as {@link JimpleUpStatement}. Otherwise, it simply uses the {@link
   * AbstractError#getLineNumber() error's line number}.
   */
  public static TextRange selectLocation(InputFile inputFile, Position position) {
    final var startLine = max(position.getFirstLine(), 1);

    var startLineOffset = position.getFirstCol();
    final var endLine = position.getLastLine();
    var endLineOffset = position.getLastCol();

    try {
      final var actualLine =
          inputFile.contents().lines().skip(max(0, startLine - 1)).findFirst().orElse("");

      if (!actualLine.isEmpty()) {
        final var actualLineOffset =
            (int)
                min(
                    actualLine.chars().takeWhile(Character::isWhitespace).count(),
                    Integer.MAX_VALUE);

        if (actualLineOffset < actualLine.length() - 1) startLineOffset = actualLineOffset;

        if (endLineOffset < 1) endLineOffset = actualLine.length();
      }
    } catch (IOException e) {
      // Ignore and continue.
    }

    return inputFile.newRange(
        startLine, startLineOffset, max(endLine - 1, startLine), endLineOffset);
  }

  public static Position intoPosition(AbstractError error) {
    final var position = intoPosition(error.getErrorStatement());
    return position != null ? position : new LinePosition(error.getLineNumber());
  }

  @Nullable
  public static Position intoPosition(Statement stmt) {
    if (stmt instanceof JimpleUpStatement upStmt) {
      return upStmt.getDelegate().getPositionInfo().getStmtPosition();
    }

    return null;
  }

  public static void joinMethods(
      MessageCrafter messageCrafter,
      @Nullable String prefixWhenMultiple,
      Collection<CrySLMethod> methods,
      @Nullable String lastDelimiter) {
    final var collected =
        methods.stream()
            .map(it -> shortNameOf(it.getDeclaringClassName(), it.getMethodName()))
            .collect(Collectors.toSet());

    if (collected.size() > 1 && prefixWhenMultiple != null) {
      messageCrafter.text(prefixWhenMultiple);
    }

    messageCrafter.joining(lastDelimiter, joiner -> collected.forEach(joiner::code));
  }
}
