package org.sonarcrypto.utils.jimple.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents the mapping of a single method argument to its position in the source code. */
public class ArgumentMapping {
  private final int argIndex;
  private final SourcePosition sourcePosition;
  private final boolean approximated;

  @JsonCreator
  public ArgumentMapping(
      @JsonProperty("argIndex") int argIndex,
      @JsonProperty("sourcePosition") SourcePosition sourcePosition,
      @JsonProperty("approximated") boolean approximated) {
    this.argIndex = argIndex;
    this.sourcePosition = sourcePosition;
    this.approximated = approximated;
  }

  /** 1-based index of the argument in the method call. */
  public int getArgIndex() {
    return argIndex;
  }

  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  /** Whether the position is an approximation rather than an exact match. */
  public boolean isApproximated() {
    return approximated;
  }

  @Override
  public String toString() {
    return "ArgumentMapping{"
        + "argIndex="
        + argIndex
        + ", sourcePosition="
        + sourcePosition
        + ", approximated="
        + approximated
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArgumentMapping that = (ArgumentMapping) o;
    return argIndex == that.argIndex
        && approximated == that.approximated
        && sourcePosition.equals(that.sourcePosition);
  }

  @Override
  public int hashCode() {
    int result = argIndex;
    result = 31 * result + sourcePosition.hashCode();
    result = 31 * result + Boolean.hashCode(approximated);
    return result;
  }
}
