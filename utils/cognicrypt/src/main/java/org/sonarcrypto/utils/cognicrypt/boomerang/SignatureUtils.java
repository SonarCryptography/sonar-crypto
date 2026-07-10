package org.sonarcrypto.utils.cognicrypt.boomerang;

import boomerang.scope.DeclaredMethod;
import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.sonar.FqClassName;

@NullMarked
public class SignatureUtils {

  private SignatureUtils() {
    // Utility class
  }

  public static String shortNameOf(DeclaredMethod method) {
    return shortNameOf(method.getDeclaringClass(), method.getName());
  }

  public static String shortNameOf(Method method) {
    return shortNameOf(method.getDeclaringClass(), method.getName());
  }

  public static String shortNameOf(FqClassName declaringClass, String methodName) {
    return shortNameOf(declaringClass.fqn(), methodName);
  }

  public static String shortNameOf(WrappedClass declaringClass, String methodName) {
    return shortNameOf(declaringClass.getFullyQualifiedName(), methodName);
  }

  public static String shortNameOf(WrappedClass clazz) {
    return shortNameOf(clazz.getFullyQualifiedName(), null);
  }

  public static String shortNameOf(FqClassName clazz) {
    return shortNameOf(clazz.fqn(), null);
  }

  public static String shortNameOf(String fqn) {
    return shortNameOf(fqn, null);
  }

  public static String shortNameOf(String declaringClassFqn, @Nullable String methodName) {
    var declaringClassName = declaringClassFqn;
    final var classLastDotIndex = declaringClassName.lastIndexOf('.');

    if (classLastDotIndex > 0) {
      declaringClassName = declaringClassName.substring(classLastDotIndex + 1);
    }

    if (methodName != null) {
      final var methodLastDotIndex = methodName.lastIndexOf('.');

      if (methodLastDotIndex > 0) methodName = methodName.substring(methodLastDotIndex + 1);
    }

    return declaringClassName + (methodName != null ? "." + methodName : "");
  }
}
