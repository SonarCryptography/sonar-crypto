package org.sonarcrypto.cognicrypt;

import org.jspecify.annotations.Nullable;

public class MavenBuildException extends Exception {
  public MavenBuildException(@Nullable String message) {
    super(message);
  }

  public MavenBuildException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
