package org.sonarcrypto.utils.jimple.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents a single mapping entry from a Jimple line number to source code position. Designed
 * with a flat structure for protobuf compatibility.
 */
public class LineMapping {
  private final int jimpleLine;
  private final ElementType elementType;
  private final String elementSignature;
  private final SourcePosition sourcePosition;
  @Nullable private final SourcePosition lhsPosition;
  @Nullable private final List<ArgumentMapping> argumentMappings;

  public LineMapping(
      int jimpleLine,
      ElementType elementType,
      String elementSignature,
      SourcePosition sourcePosition) {
    this(jimpleLine, elementType, elementSignature, sourcePosition, null, null);
  }

  @JsonCreator
  public LineMapping(
      @JsonProperty("jimpleLine") int jimpleLine,
      @JsonProperty("elementType") ElementType elementType,
      @JsonProperty("elementSignature") String elementSignature,
      @JsonProperty("sourcePosition") SourcePosition sourcePosition,
      @JsonProperty("lhsPosition") @Nullable SourcePosition lhsPosition,
      @JsonProperty("argumentMappings") @Nullable List<ArgumentMapping> argumentMappings) {
    this.jimpleLine = jimpleLine;
    this.elementType = elementType;
    this.elementSignature = elementSignature;
    this.sourcePosition = sourcePosition;
    this.lhsPosition = lhsPosition;
    this.argumentMappings =
        argumentMappings != null
            ? Collections.unmodifiableList(new ArrayList<>(argumentMappings))
            : null;
  }

  public int getJimpleLine() {
    return jimpleLine;
  }

  public ElementType getElementType() {
    return elementType;
  }

  public String getElementSignature() {
    return elementSignature;
  }

  public SourcePosition getSourcePosition() {
    return sourcePosition;
  }

  /** Position of the left-hand side of an assignment, or null if not applicable. */
  @Nullable
  public SourcePosition getLhsPosition() {
    return lhsPosition;
  }

  /**
   * Positions of individual arguments in a method call, or null if not available. Each entry maps a
   * 1-based argument index to its source position.
   */
  @Nullable
  public List<ArgumentMapping> getArgumentMappings() {
    return argumentMappings;
  }

  @Override
  public String toString() {
    return "LineMapping{"
        + "jimpleLine="
        + jimpleLine
        + ", elementType="
        + elementType
        + ", elementSignature='"
        + elementSignature
        + '\''
        + ", sourcePosition="
        + sourcePosition
        + (lhsPosition != null ? ", lhsPosition=" + lhsPosition : "")
        + (argumentMappings != null ? ", argumentMappings=" + argumentMappings : "")
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LineMapping that = (LineMapping) o;
    return jimpleLine == that.jimpleLine
        && elementType == that.elementType
        && elementSignature.equals(that.elementSignature)
        && sourcePosition.equals(that.sourcePosition);
  }

  @Override
  public int hashCode() {
    int result = jimpleLine;
    result = 31 * result + elementType.hashCode();
    result = 31 * result + elementSignature.hashCode();
    result = 31 * result + sourcePosition.hashCode();
    return result;
  }
}
