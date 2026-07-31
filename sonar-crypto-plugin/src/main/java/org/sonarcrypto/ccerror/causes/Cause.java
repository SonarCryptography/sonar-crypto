package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public abstract sealed class Cause permits ValueCause, CallCause {

  public abstract void createMessage(MessageCrafter messageCrafter);
}
