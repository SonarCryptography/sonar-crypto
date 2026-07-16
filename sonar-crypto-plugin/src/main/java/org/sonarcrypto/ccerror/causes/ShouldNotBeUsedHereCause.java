package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class ShouldNotBeUsedHereCause extends ValueCause {
  @Override
  public void createMessage(MessageCrafter messageCrafter) {
    messageCrafter.text("should not be used here.");
  }

  @Override
  public String toString() {
    return "ShouldNotBeUsedHereCause";
  }
}
