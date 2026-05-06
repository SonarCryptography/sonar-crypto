package org.sonarcrypto.ccerror.violations.reasons;

import static org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils.shortNameOf;
import static org.sonarcrypto.utils.sonar.TextUtils.code;

import boomerang.scope.DeclaredMethod;
import crysl.rule.CrySLMethod;
import java.util.Collection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.sonar.TextUtils;

@NullMarked
public final class UnexpectedCallReason extends CallReason {
  private final DeclaredMethod unexpectedMethod;
  private final Collection<CrySLMethod> expectedMethods;

  public UnexpectedCallReason(
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
  public void createMessage(StringBuilder messageBuilder) {
    final var unexpectedMethod = getUnexpectedMethod();
    final var expectedMethods = getExpectedMethods();

    messageBuilder
        .append("Unexpected call to method ")
        .append(
            code(
                shortNameOf(
                    unexpectedMethod.getDeclaringClass().getFullyQualifiedName(),
                    unexpectedMethod.getName())))
        .append('.');

    if (!expectedMethods.isEmpty()) {
      messageBuilder
          .append(" Expected calling either ")
          .append(
              TextUtils.join(
                  expectedMethods.stream()
                      .map(
                          it ->
                              code(
                                  shortNameOf(
                                      it.getDeclaringClassName(), it.getShortMethodName()))),
                  "or"))
          .append(".");
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnexpectedCallReason that = (UnexpectedCallReason) o;
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
    return "UnexpectedCallReason{"
        + "unexpectedMethod="
        + unexpectedMethod
        + ", expectedMethods="
        + expectedMethods
        + '}';
  }
}
