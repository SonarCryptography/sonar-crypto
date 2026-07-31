package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class ImproperGeneratedCause extends ValueCause {
  @Override
  public void createMessage(MessageCrafter messageCrafter) {
    messageCrafter.text("was cryptographically improper generated.");
  }

  @Override
  public String toString() {
    return "ImproperGeneratedCause";
  }
}
