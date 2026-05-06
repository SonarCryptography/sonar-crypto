package org.sonarcrypto.ccerror.violations.reasons;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract sealed class CallReason extends Reason
    permits ForbiddenMethodReason,
        IncompleteOperationReason,
        UncaughtExceptionReason,
        UnexpectedCallReason {

  public abstract void createMessage(StringBuilder messageBuilder);
}
