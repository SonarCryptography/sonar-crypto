package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract sealed class CallCause extends Cause
    permits ForbiddenMethodCause,
        IncompleteOperationCause,
        UncaughtExceptionCause,
        UnexpectedCallCause {

  public abstract void createMessage(StringBuilder messageBuilder);
}
