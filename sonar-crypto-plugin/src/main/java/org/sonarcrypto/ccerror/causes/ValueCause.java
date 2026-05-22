package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract sealed class ValueCause extends Cause
    permits InvalidValueCause,
        ImproperGeneratedCause,
        ForbiddenTypeCause,
        ShouldNotBeUsedHereCause,
        UndefinedCause {

  public abstract void createMessage(StringBuilder messageBuilder);
}
