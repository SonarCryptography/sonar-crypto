package org.sonarcrypto.utils.jimple.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collection of line mappings for a single class, with metadata.
 * Designed with a flat structure for protobuf compatibility.
 */
public class LineMappingCollection {
  private final String className;
  @Nullable private final String sourceFileName;
  private final List<LineMapping> mappings;

  public LineMappingCollection(String className, @Nullable String sourceFileName, List<LineMapping> mappings) {
    this.className = className;
    this.sourceFileName = sourceFileName;
    this.mappings = new ArrayList<>(mappings);
  }

  public String getClassName() {
    return className;
  }

  @Nullable
  public String getSourceFileName() {
    return sourceFileName;
  }

  public List<LineMapping> getMappings() {
    return Collections.unmodifiableList(mappings);
  }

  public int size() {
    return mappings.size();
  }

  /**
   * Converts this collection to JSON string.
   *
   * @return JSON representation of this collection
   * @throws IOException if serialization fails
   */
  public String toJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    return mapper.writeValueAsString(this);
  }

  /**
   * Writes this collection as JSON to the given writer.
   *
   * @param writer The writer to write to
   * @throws IOException if writing fails
   */
  public void writeJson(Writer writer) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(writer, this);
  }

  @Override
  public String toString() {
    return "LineMappingCollection{"
        + "className='"
        + className
        + '\''
        + ", sourceFileName='"
        + sourceFileName
        + '\''
        + ", mappings="
        + mappings.size()
        + " entries}";
  }
}

