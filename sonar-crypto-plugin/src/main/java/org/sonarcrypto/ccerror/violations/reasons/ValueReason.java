package org.sonarcrypto.ccerror.violations.reasons;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract sealed class ValueReason extends Reason
    permits InvalidValuesReason,
        ImproperGeneratedReason,
        ForbiddenTypeReason,
        ShouldNotBeUsedHereReason,
        UndefinedReason {

  public abstract void createMessage(StringBuilder messageBuilder);
}
