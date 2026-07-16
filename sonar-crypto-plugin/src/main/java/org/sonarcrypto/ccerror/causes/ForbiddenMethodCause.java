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
public final class ForbiddenMethodCause extends CallCause {

  private final DeclaredMethod forbiddenMethod;
  private final Collection<CrySLMethod> alternatives;

  public ForbiddenMethodCause(
      DeclaredMethod forbiddenMethod, Collection<CrySLMethod> alternatives) {
    this.forbiddenMethod = forbiddenMethod;
    this.alternatives = alternatives;
  }

  public DeclaredMethod getForbiddenMethod() {
    return this.forbiddenMethod;
  }

  public Collection<CrySLMethod> getAlternatives() {
    return this.alternatives;
  }

  @Override
  public void createMessage(MessageCrafter messageCrafter) {

    messageCrafter
        .text("Call to the prohibited method ")
        .code(
            shortNameOf(
                forbiddenMethod.getDeclaringClass().getFullyQualifiedName(),
                forbiddenMethod.getName()))
        .text(".");

    if (!alternatives.isEmpty()) {
      messageCrafter.text(" Consider calling ");
      ConverterUtils.joinMethods(messageCrafter, "either", alternatives, ", or ");
      messageCrafter.text(" instead.");
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ForbiddenMethodCause that = (ForbiddenMethodCause) o;
    return forbiddenMethod.equals(that.forbiddenMethod) && alternatives.equals(that.alternatives);
  }

  @Override
  public int hashCode() {
    int result = forbiddenMethod.hashCode();
    result = 31 * result + alternatives.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ForbiddenMethodCause{"
        + "forbiddenMethod="
        + forbiddenMethod
        + ", alternatives="
        + alternatives
        + '}';
  }
}
