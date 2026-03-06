package org.sonarcrypto.ccerror;

import static java.lang.Math.*;

import boomerang.scope.sootup.jimple.JimpleUpStatement;
import crypto.analysis.errors.AbstractError;
import crypto.utils.CrySLUtils;
import java.io.IOException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;
import org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils;

public class ConverterUtils {
  /**
   * Stringifies the callee from a callee info, e.g., {@code `Foo.bar`}. Constructor and static
   * constructor are handled differently, e.g., {@code `Foo`'s constructor}. If the callee info is
   * {@code null}, the string {@code the callee} is returned.
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static String stringifyCallee(Optional<CalleeInfo> calleeInfo) {
    return stringifyCallee(calleeInfo.orElse(null));
  }

  /**
   * Stringifies the callee from a callee info, e.g., {@code `Foo.bar`}. Constructor and static
   * constructor are handled differently, e.g., {@code `Foo`'s constructor}. If the callee info is
   * {@code null}, the string {@code the callee} is returned.
   */
  public static String stringifyCallee(@Nullable CalleeInfo calleeInfo) {
    if (calleeInfo == null) {
      return "the callee";
    }

    final var name = calleeInfo.methodName();

    return switch (name) {
      case "<init>" -> SignatureUtils.shortNameOf(calleeInfo.className()) + "'s constructor";
      case "<clinit>" ->
          SignatureUtils.shortNameOf(calleeInfo.className()) + "'s static constructor";
      default -> SignatureUtils.shortNameOf(calleeInfo.className(), calleeInfo.methodName());
    };
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
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static String stringifyArgumentIndex(
      int zeroBasedArgumentIndex, Optional<Integer> parameterCount) {
    return stringifyArgumentIndex(zeroBasedArgumentIndex, parameterCount.orElse(null));
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
  public static String stringifyArgumentIndex(
      int zeroBasedArgumentIndex, @Nullable Integer parameterCount) {
    if (zeroBasedArgumentIndex < 0 || parameterCount != null && parameterCount == 1)
      return "argument";

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
  public static TextRange selectLocation(InputFile inputFile, AbstractError error) {
    final var stmt = error.getErrorStatement();

    if (stmt instanceof JimpleUpStatement upStmt) {
      final var positionInfo = upStmt.getDelegate().getPositionInfo();

      final var position = positionInfo.getStmtPosition();

      final var startLine = max(position.getFirstLine(), 1);
      var startLineOffset = position.getFirstCol();
      final var endLine = position.getLastLine();
      var endLineOffset = position.getLastCol();

      if (startLineOffset < 0 && endLine < 1 && endLineOffset < 1) {
        try {
          final var actualLine =
              inputFile.contents().lines().skip(max(0, startLine - 1)).findFirst().orElse("");

          if (!actualLine.isEmpty()) {
            final var actualLineOffset =
                (int)
                    min(
                        actualLine.chars().takeWhile(Character::isWhitespace).count(),
                        Integer.MAX_VALUE);

            endLineOffset = actualLine.length();
            if (actualLineOffset < actualLine.length() - 1) startLineOffset = actualLineOffset;
          }
        } catch (IOException e) {
          // Ignore and continue.
        }
      }

      if (endLineOffset < 1) {
        return inputFile.selectLine(startLine);
      }

      return inputFile.newRange(
          startLine, startLineOffset, max(endLine - 1, startLine), endLineOffset);
    }

    return inputFile.selectLine(error.getLineNumber());
  }
}
