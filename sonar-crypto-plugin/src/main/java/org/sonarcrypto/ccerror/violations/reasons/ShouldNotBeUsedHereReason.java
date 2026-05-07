package org.sonarcrypto.ccerror.violations.reasons;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ShouldNotBeUsedHereReason extends ValueReason {
  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder.append("should not be used here.");
  }

  @Override
  public String toString() {
    return "ShouldNotBeUsedHereReason";
  }
}
