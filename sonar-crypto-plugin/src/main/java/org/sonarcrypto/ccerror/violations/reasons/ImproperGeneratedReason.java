package org.sonarcrypto.ccerror.violations.reasons;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ImproperGeneratedReason extends ValueReason {
  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder.append("was cryptographically improper generated.");
  }

  @Override
  public String toString() {
    return "ImproperGeneratedReason";
  }
}
