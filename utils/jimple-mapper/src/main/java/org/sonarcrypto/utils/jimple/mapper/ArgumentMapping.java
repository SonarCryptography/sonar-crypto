package org.sonarcrypto.utils.jimple.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Represents the mapping of a single method argument to its position in the source code. */
@NullMarked
public class ArgumentMapping {
  private final int argIndex;
  private final SourcePosition sourcePosition;

  @JsonCreator
  public ArgumentMapping(
      @JsonProperty("argIndex") int argIndex,
      @JsonProperty("sourcePosition") SourcePosition sourcePosition) {
    this.argIndex = argIndex;
    this.sourcePosition = sourcePosition;
  }

  /** 1-based index of the argument in the method call. */
  public int getArgIndex() {
    return argIndex;
  }

  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  @Override
  public String toString() {
    return "ArgumentMapping{" + "argIndex=" + argIndex + ", sourcePosition=" + sourcePosition + '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArgumentMapping that = (ArgumentMapping) o;
    return argIndex == that.argIndex && sourcePosition.equals(that.sourcePosition);
  }

  @Override
  public int hashCode() {
    int result = argIndex;
    result = 31 * result + sourcePosition.hashCode();
    return result;
  }
}
