package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public abstract sealed class CallCause extends Cause
    permits ForbiddenMethodCause,
        IncompleteOperationCause,
        UncaughtExceptionCause,
        UnexpectedCallCause {

  public abstract void createMessage(MessageCrafter messageCrafter);
}
