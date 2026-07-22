package org.sonarcrypto.ccerror.causes;

import crysl.rule.CrySLMethod;
import java.util.Collection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class IncompleteOperationCause extends CallCause {
  private final IncompleteObject incompleteObject;
  private final Collection<crysl.rule.CrySLMethod> expectedMethods;

  public IncompleteOperationCause(
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
  public void createMessage(MessageCrafter messageCrafter) {
    messageCrafter.text("Incomplete operation on ");
    getIncompleteObject().createMessage(messageCrafter);
    messageCrafter.text(".");

    if (!expectedMethods.isEmpty()) {
      messageCrafter
          .text(" Expected call to ")
          .methodsJoined("either", expectedMethods, "or")
          .text(".");
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IncompleteOperationCause that = (IncompleteOperationCause) o;
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
    return "IncompleteOperationCause{"
        + "incompleteObject='"
        + incompleteObject
        + '\''
        + ", expectedMethods="
        + expectedMethods
        + '}';
  }

  public abstract static sealed class IncompleteObject {
    public abstract void createMessage(MessageCrafter messageCrafter);
  }

  public static final class UntypedIncompleteObject extends IncompleteObject {
    @Override
    public void createMessage(MessageCrafter messageCrafter) {
      messageCrafter.text(" object ");
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
    public void createMessage(MessageCrafter messageCrafter) {
      messageCrafter.text(" object of type ").code(getClassName());
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
