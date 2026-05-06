package org.sonarcrypto.ccerror.violations.reasons;

import static org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils.shortNameOf;
import static org.sonarcrypto.utils.sonar.TextUtils.code;

import boomerang.scope.DeclaredMethod;
import crysl.rule.CrySLMethod;
import java.util.Collection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils;

@NullMarked
public final class ForbiddenMethodReason extends CallReason {

  private final DeclaredMethod forbiddenMethod;
  private final Collection<CrySLMethod> alternatives;

  public ForbiddenMethodReason(
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
  public void createMessage(StringBuilder messageBuilder) {
    final var forbiddenMethod = this.getForbiddenMethod();
    final var alternatives = this.getAlternatives();

    messageBuilder
        .append("Call to the prohibited method ")
        .append(
            code(
                shortNameOf(
                    forbiddenMethod.getDeclaringClass().getFullyQualifiedName(),
                    forbiddenMethod.getName())))
        .append('.');

    if (!alternatives.isEmpty()) {
      messageBuilder
          .append(" Consider calling ")
          .append(ConverterUtils.joinMethods("either", alternatives, "or"))
          .append(" instead.");
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ForbiddenMethodReason that = (ForbiddenMethodReason) o;
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
    return "ForbiddenMethodReason{"
        + "forbiddenMethod="
        + forbiddenMethod
        + ", alternatives="
        + alternatives
        + '}';
  }
}
