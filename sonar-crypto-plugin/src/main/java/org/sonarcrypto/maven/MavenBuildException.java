package org.sonarcrypto.maven;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class MavenBuildException extends Exception {
  public MavenBuildException(@Nullable String message) {
    super(message);
  }

  public MavenBuildException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
