package org.sonarcrypto.utility.groundtruth;

import java.io.IOException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GroundTruthParsingException extends IOException {
  public GroundTruthParsingException(String message) {
    super(message);
  }
}
