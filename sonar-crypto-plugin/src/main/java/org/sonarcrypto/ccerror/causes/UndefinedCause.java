package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class UndefinedCause extends ValueCause {

  private final String message;

  public UndefinedCause(String message) {
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder.append(getMessage());
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
