package org.sonarcrypto.ccerror.causes;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class ForbiddenTypeCause extends ValueCause {

  private final String disallowedType;

  public ForbiddenTypeCause(String disallowedType) {
    this.disallowedType = disallowedType;
  }

  public String getDisallowedType() {
    return this.disallowedType;
  }

  @Override
  public void createMessage(MessageCrafter messageCrafter) {
    messageCrafter.text("should never be of the type ").code(getDisallowedType()).text(".");
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ForbiddenTypeCause that = (ForbiddenTypeCause) o;
    return disallowedType.equals(that.disallowedType);
  }

  @Override
  public int hashCode() {
    return disallowedType.hashCode();
  }

  @Override
  public String toString() {
    return "ForbiddenTypeCause{" + "disallowedType=" + disallowedType + '}';
  }
}
