package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.sonar.messagecrafter.CraftedMessage;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class UndefinedCause extends ValueCause {

  private final CraftedMessage message;

  public UndefinedCause(CraftedMessage message) {
    this.message = message;
  }

  public UndefinedCause(String message) {
    this(new CraftedMessage(message));
  }

  public CraftedMessage getMessage() {
    return this.message;
  }

  @Override
  public void createMessage(MessageCrafter messageCrafter) {
    messageCrafter.append(getMessage());
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UndefinedCause that = (UndefinedCause) o;
    return message.equals(that.message);
  }

  @Override
  public int hashCode() {
    return message.hashCode();
  }

  @Override
  public String toString() {
    return "UndefinedCause{" + "message=" + message + '}';
  }
}
