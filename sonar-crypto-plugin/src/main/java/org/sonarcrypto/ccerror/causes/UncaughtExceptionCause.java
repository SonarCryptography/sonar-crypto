package org.sonarcrypto.ccerror.causes;

import static org.sonarcrypto.utils.sonar.TextUtils.code;

import boomerang.scope.WrappedClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class UncaughtExceptionCause extends CallCause {

  private final WrappedClass uncaughtException;

  public UncaughtExceptionCause(WrappedClass uncaughtException) {
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

    UncaughtExceptionCause that = (UncaughtExceptionCause) o;
    return uncaughtException.equals(that.uncaughtException);
  }

  @Override
  public int hashCode() {
    return uncaughtException.hashCode();
  }

  @Override
  public String toString() {
    return "UncaughtExceptionCause{" + "uncaughtException=" + uncaughtException + '}';
  }
}
