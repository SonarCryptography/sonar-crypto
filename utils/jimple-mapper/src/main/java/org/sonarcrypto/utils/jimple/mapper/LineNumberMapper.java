package org.sonarcrypto.utils.jimple.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import sootup.core.model.Position;

/**
 * Collects line number mappings from Jimple code to source positions. Records mappings for classes,
 * methods, fields, and statements.
 */
public class LineNumberMapper {
  private final String className;
  private final List<LineMapping> mappings;
  @Nullable private String sourceFileName;

  public LineNumberMapper(String className) {
    this.className = className;
    this.mappings = new ArrayList<>();
    this.sourceFileName = null;
  }

  /**
   * Sets the source file name for this class. Should be called once when the source file is known.
   *
   * @param sourceFileName The source file name (just filename, not full path)
   */
  public void setSourceFileName(String sourceFileName) {
    this.sourceFileName = sourceFileName;
  }

  /**
   * Records a class position mapping.
   *
   * @param jimpleLine The line number in the generated Jimple code
   * @param signature The class signature
   * @param position The position in the source code
   */
  public void recordClassPosition(int jimpleLine, String signature, Position position) {
    SourcePosition sourcePos = convertPosition(position);
    mappings.add(new LineMapping(jimpleLine, ElementType.CLASS, signature, sourcePos));
  }

  /**
   * Records a method position mapping.
   *
   * @param jimpleLine The line number in the generated Jimple code
   * @param signature The method signature
   * @param position The position in the source code
   */
  public void recordMethodPosition(int jimpleLine, String signature, Position position) {
    SourcePosition sourcePos = convertPosition(position);
    mappings.add(new LineMapping(jimpleLine, ElementType.METHOD, signature, sourcePos));
  }

  /**
   * Records a field position mapping.
   *
   * @param jimpleLine The line number in the generated Jimple code
   * @param signature The field signature
   * @param position The position in the source code
   */
  public void recordFieldPosition(int jimpleLine, String signature, Position position) {
    SourcePosition sourcePos = convertPosition(position);
    mappings.add(new LineMapping(jimpleLine, ElementType.FIELD, signature, sourcePos));
  }

  /**
   * Records a statement position mapping.
   *
   * @param jimpleLine The line number in the generated Jimple code
   * @param stmtString The statement as string (for identification)
   * @param position The position in the source code
   */
  public void recordStmtPosition(int jimpleLine, String stmtString, Position position) {
    SourcePosition sourcePos = convertPosition(position);
    mappings.add(new LineMapping(jimpleLine, ElementType.STATEMENT, stmtString, sourcePos));
  }

  /**
   * Converts a SootUp Position to our SourcePosition representation.
   *
   * @param position The SootUp position
   * @return The converted source position
   */
  private SourcePosition convertPosition(Position position) {
    return new SourcePosition(
        position.getFirstLine(),
        position.getLastLine(),
        position.getFirstCol(),
        position.getLastCol());
  }

  /**
   * Returns the collected mappings as a LineMappingCollection.
   *
   * @return The collection of line mappings
   */
  public LineMappingCollection getCollection() {
    return new LineMappingCollection(className, sourceFileName, mappings);
  }

  /**
   * Returns the number of mappings recorded.
   *
   * @return The number of mappings
   */
  public int size() {
    return mappings.size();
  }
}
