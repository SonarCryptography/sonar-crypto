package org.sonarcrypto.ccerror.violations.reasons;

import static org.sonarcrypto.utils.sonar.TextUtils.code;

import crysl.rule.CrySLMethod;
import java.util.Collection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils;

@NullMarked
public final class IncompleteOperationReason extends CallReason {
  private final IncompleteObject incompleteObject;
  private final Collection<crysl.rule.CrySLMethod> expectedMethods;

  public IncompleteOperationReason(
      IncompleteObject incompleteObject, Collection<CrySLMethod> expectedMethods) {
    this.incompleteObject = incompleteObject;
    this.expectedMethods = expectedMethods;
  }

  public IncompleteObject getIncompleteObject() {
    return this.incompleteObject;
  }

  public Collection<CrySLMethod> getExpectedMethods() {
    return this.expectedMethods;
  }

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder.append("Incomplete operation on ");
    getIncompleteObject().createMessage(messageBuilder);
    messageBuilder.append('.');

    if (!expectedMethods.isEmpty()) {
      messageBuilder
          .append(" Expected call to ")
          .append(ConverterUtils.joinMethods("either", expectedMethods, "or"))
          .append(".");
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IncompleteOperationReason that = (IncompleteOperationReason) o;
    return incompleteObject.equals(that.incompleteObject)
        && expectedMethods.equals(that.expectedMethods);
  }

  @Override
  public int hashCode() {
    int result = incompleteObject.hashCode();
    result = 31 * result + expectedMethods.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "IncompleteOperationReason{"
        + "incompleteObject='"
        + incompleteObject
        + '\''
        + ", expectedMethods="
        + expectedMethods
        + '}';
  }

  public abstract static sealed class IncompleteObject {
    public abstract void createMessage(StringBuilder messageBuilder);
  }

  public static final class UntypedIncompleteObject extends IncompleteObject {
    @Override
    public void createMessage(StringBuilder messageBuilder) {
      messageBuilder.append(" object ");
    }

    @Override
    public String toString() {
      return "UntypedIncompleteObject";
    }
  }

  public static final class TypedIncompleteObject extends IncompleteObject {
    private final String className;

    public TypedIncompleteObject(String className) {
      this.className = className;
    }

    public String getClassName() {
      return this.className;
    }

    @Override
    public void createMessage(StringBuilder messageBuilder) {
      messageBuilder.append(" object of type ").append(code(getClassName()));
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TypedIncompleteObject that = (TypedIncompleteObject) o;
      return className.equals(that.className);
    }

    @Override
    public int hashCode() {
      return className.hashCode();
    }

    @Override
    public String toString() {
      return "TypedIncompleteObject{" + "className='" + className + '\'' + '}';
    }
  }
}
