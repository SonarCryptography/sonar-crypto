package org.sonarcrypto.ccerror;

import static java.lang.Math.max;

import boomerang.scope.sootup.jimple.JimpleUpStatement;
import crypto.analysis.errors.AbstractError;
import crypto.utils.CrySLUtils;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;

public class ConverterUtils {
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

      final var startLine = position.getFirstLine();
      final var startLineOffset = position.getFirstCol();
      final var endLine = position.getLastLine();
      final var endLineOffset = position.getLastCol();

      if (startLine < 1) {
        return inputFile.selectLine(1);
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
