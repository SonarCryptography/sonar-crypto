package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract sealed class Cause permits ValueCause, CallCause {

  public abstract void createMessage(StringBuilder messageBuilder);
}
