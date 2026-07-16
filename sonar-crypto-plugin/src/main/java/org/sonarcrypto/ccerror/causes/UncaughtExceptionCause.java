package org.sonarcrypto.ccerror.causes;

import boomerang.scope.WrappedClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

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
  public void createMessage(MessageCrafter messageCrafter) {
    messageCrafter
        .text("Uncaught exception ")
        .code(this.getUncaughtException().getFullyQualifiedName())
        .text(".");
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
