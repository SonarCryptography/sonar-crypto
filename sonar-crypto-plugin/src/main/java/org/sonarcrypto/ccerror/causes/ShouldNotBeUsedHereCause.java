package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ShouldNotBeUsedHereCause extends ValueCause {
  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder.append("should not be used here.");
  }

  @Override
  public String toString() {
    return "ShouldNotBeUsedHereCause";
  }
}
