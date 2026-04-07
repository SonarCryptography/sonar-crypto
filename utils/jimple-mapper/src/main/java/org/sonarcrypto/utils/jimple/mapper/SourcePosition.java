package org.sonarcrypto.utils.jimple.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import sootup.core.model.FullPosition;
import sootup.core.model.Position;

/**
 * Represents a position in source code with line and column information. Designed with a flat
 * structure for protobuf compatibility.
 */
public class SourcePosition {
  private final int firstLine;
  private final int lastLine;
  private final int firstCol;
  private final int lastCol;

  @JsonCreator
  public SourcePosition(
      @JsonProperty("firstLine") int firstLine,
      @JsonProperty("lastLine") int lastLine,
      @JsonProperty("firstCol") int firstCol,
      @JsonProperty("lastCol") int lastCol) {
    this.firstLine = firstLine;
    this.lastLine = lastLine;
    this.firstCol = firstCol;
    this.lastCol = lastCol;
  }

  public int getFirstLine() {
    return firstLine;
  }

  public int getLastLine() {
    return lastLine;
  }

  public int getFirstCol() {
    return firstCol;
  }

  public int getLastCol() {
    return lastCol;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(firstLine);
    if (firstCol >= 0) {
      sb.append(":").append(firstCol);
    }
    sb.append("-").append(lastLine);
    if (lastCol >= 0) {
      sb.append(":").append(lastCol);
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SourcePosition that = (SourcePosition) o;
    return firstLine == that.firstLine
        && lastLine == that.lastLine
        && firstCol == that.firstCol
        && lastCol == that.lastCol;
  }

  @Override
  public int hashCode() {
    int result = firstLine;
    result = 31 * result + lastLine;
    result = 31 * result + firstCol;
    result = 31 * result + lastCol;
    return result;
  }

  public Position toSootUpPosition() {
    return new FullPosition(firstLine, firstCol, lastLine, lastCol);
  }
}
