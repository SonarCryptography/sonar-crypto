package org.sonarcrypto.utils.jimple.mapper;

/**
 * Represents a single mapping entry from a Jimple line number to source code position.
 * Designed with a flat structure for protobuf compatibility.
 */
public class LineMapping {
  private final int jimpleLine;
  private final ElementType elementType;
  private final String elementSignature;
  private final SourcePosition sourcePosition;

  public LineMapping(
      int jimpleLine,
      ElementType elementType,
      String elementSignature,
      SourcePosition sourcePosition) {
    this.jimpleLine = jimpleLine;
    this.elementType = elementType;
    this.elementSignature = elementSignature;
    this.sourcePosition = sourcePosition;
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

