package org.sonarcrypto.utils.cognicrypt.jimple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class UnsupportedClassSourceException extends RuntimeException {
  public UnsupportedClassSourceException(String classSourceTypeName) {
    super("Unsupported class source type: " + classSourceTypeName);
  }
}
