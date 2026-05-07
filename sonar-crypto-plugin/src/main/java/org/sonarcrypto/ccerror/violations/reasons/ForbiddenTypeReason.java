package org.sonarcrypto.ccerror.violations.reasons;

import static org.sonarcrypto.utils.sonar.TextUtils.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ForbiddenTypeReason extends ValueReason {

  private final String disallowedType;

  public ForbiddenTypeReason(String disallowedType) {
    this.disallowedType = disallowedType;
  }

  public String getDisallowedType() {
    return this.disallowedType;
  }

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder
        .append("should never be of the type ")
        .append(quote(getDisallowedType()))
        .append(".");
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ForbiddenTypeReason that = (ForbiddenTypeReason) o;
    return disallowedType.equals(that.disallowedType);
  }

  @Override
  public int hashCode() {
    return disallowedType.hashCode();
  }

  @Override
  public String toString() {
    return "ForbiddenTypeReason{" + "disallowedType=" + disallowedType + '}';
  }
}
