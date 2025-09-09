package org.sonarcrypto.cognicrypt;

public class MavenBuildException extends Exception {
  public MavenBuildException(String message) {
    super(message);
  }

  public MavenBuildException(String message, Throwable cause) {
    super(message, cause);
  }
}
