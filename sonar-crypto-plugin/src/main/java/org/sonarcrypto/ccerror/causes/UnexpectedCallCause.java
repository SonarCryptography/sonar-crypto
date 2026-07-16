package org.sonarcrypto.ccerror.causes;

import static org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils.shortNameOf;

import boomerang.scope.DeclaredMethod;
import crysl.rule.CrySLMethod;
import java.util.Collection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class UnexpectedCallCause extends CallCause {
  private final DeclaredMethod unexpectedMethod;
  private final Collection<CrySLMethod> expectedMethods;

  public UnexpectedCallCause(
      DeclaredMethod unexpectedMethod, Collection<CrySLMethod> expectedMethods) {
    this.unexpectedMethod = unexpectedMethod;
    this.expectedMethods = expectedMethods;
  }

  public DeclaredMethod getUnexpectedMethod() {
    return this.unexpectedMethod;
  }

  public Collection<CrySLMethod> getExpectedMethods() {
    return this.expectedMethods;
  }

  @Override
  public void createMessage(MessageCrafter messageCrafter) {
    messageCrafter
        .text("Unexpected call to method ")
        .code(
            shortNameOf(
                unexpectedMethod.getDeclaringClass().getFullyQualifiedName(),
                unexpectedMethod.getName()))
        .text(".");

    if (!expectedMethods.isEmpty()) {
      messageCrafter.text(" Expected calling ");
      ConverterUtils.joinMethods(messageCrafter, " either ", expectedMethods, ", or ");
      messageCrafter.text(".");
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnexpectedCallCause that = (UnexpectedCallCause) o;
    return unexpectedMethod.equals(that.unexpectedMethod)
        && expectedMethods.equals(that.expectedMethods);
  }

  @Override
  public int hashCode() {
    int result = unexpectedMethod.hashCode();
    result = 31 * result + expectedMethods.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "UnexpectedCallCause{"
        + "unexpectedMethod="
        + unexpectedMethod
        + ", expectedMethods="
        + expectedMethods
        + '}';
  }
}
