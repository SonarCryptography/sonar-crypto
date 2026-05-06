package org.sonarcrypto.ccerror.violations.reasons;

import static org.sonarcrypto.utils.sonar.TextUtils.code;

import boomerang.scope.WrappedClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class UncaughtExceptionReason extends CallReason {

  private final WrappedClass uncaughtException;

  public UncaughtExceptionReason(WrappedClass uncaughtException) {
    this.uncaughtException = uncaughtException;
  }

  public WrappedClass getUncaughtException() {
    return this.uncaughtException;
  }

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder.append(
        String.format(
            "Uncaught exception %s.", code(this.getUncaughtException().getFullyQualifiedName())));
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UncaughtExceptionReason that = (UncaughtExceptionReason) o;
    return uncaughtException.equals(that.uncaughtException);
  }

  @Override
  public int hashCode() {
    return uncaughtException.hashCode();
  }

  @Override
  public String toString() {
    return "UncaughtExceptionReason{" + "uncaughtException=" + uncaughtException + '}';
  }
}
