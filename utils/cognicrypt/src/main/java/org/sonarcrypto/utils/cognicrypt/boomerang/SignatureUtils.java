package org.sonarcrypto.utils.cognicrypt.boomerang;

import boomerang.scope.DeclaredMethod;
import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SignatureUtils {
  public static String shortNameOf(DeclaredMethod method) {
    return shortNameOf(method.getDeclaringClass(), method.getName());
  }

  public static String shortNameOf(Method method) {
    return shortNameOf(method.getDeclaringClass(), method.getName());
  }

  private static String shortNameOf(WrappedClass declaringClass, String methodName) {
    return shortNameOf(declaringClass.getFullyQualifiedName(), methodName);
  }

  public static String shortNameOf(WrappedClass clazz) {
    return shortNameOf(clazz.getFullyQualifiedName(), null);
  }

  public static String shortNameOf(String fqn) {
    return shortNameOf(fqn, null);
  }

  public static String shortNameOf(String declaringClassFqn, @Nullable String methodName) {
    var declaringClassName = declaringClassFqn;
    final var lastDotIndex = declaringClassName.lastIndexOf('.');

    if (lastDotIndex > 0) {
      declaringClassName = declaringClassName.substring(lastDotIndex + 1);
    }

    return "`" + declaringClassName + (methodName != null ? "." + methodName : "") + "`";
  }
}
