package org.sonarcrypto.ccerror.violations.reasons;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract sealed class Reason permits ValueReason, CallReason {

  public abstract void createMessage(StringBuilder messageBuilder);
}
