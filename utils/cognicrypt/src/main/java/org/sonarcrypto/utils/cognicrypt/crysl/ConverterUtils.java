package org.sonarcrypto.utils.cognicrypt.crysl;

import static java.lang.Math.*;

import boomerang.scope.Statement;
import boomerang.scope.sootup.jimple.JimpleUpStatement;
import crypto.analysis.errors.AbstractError;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import sootup.core.model.LinePosition;
import sootup.core.model.Position;

public class ConverterUtils {

  private ConverterUtils() {
    // Utility class
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
}
