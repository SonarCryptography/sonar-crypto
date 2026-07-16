package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public abstract sealed class ValueCause extends Cause
    permits InvalidValueCause,
        ImproperGeneratedCause,
        ForbiddenTypeCause,
        ShouldNotBeUsedHereCause,
        UndefinedCause {

  public abstract void createMessage(MessageCrafter messageCrafter);
}
