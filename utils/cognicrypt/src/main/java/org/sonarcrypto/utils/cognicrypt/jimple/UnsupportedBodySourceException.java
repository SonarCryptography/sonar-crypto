package org.sonarcrypto.utils.cognicrypt.jimple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class UnsupportedBodySourceException extends RuntimeException {
  public UnsupportedBodySourceException(String message) {
    super(message);
  }
}
