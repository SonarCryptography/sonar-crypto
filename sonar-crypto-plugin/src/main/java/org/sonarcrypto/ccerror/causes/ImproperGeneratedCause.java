package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ImproperGeneratedCause extends ValueCause {
  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder.append("was cryptographically improper generated.");
  }

  @Override
  public String toString() {
    return "ImproperGeneratedCause";
  }
}
