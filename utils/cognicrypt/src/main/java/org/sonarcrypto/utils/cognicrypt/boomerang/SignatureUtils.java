package org.sonarcrypto.utils.cognicrypt.boomerang;

import boomerang.scope.DeclaredMethod;
import boomerang.scope.Method;
import boomerang.scope.WrappedClass;

public class SignatureUtils {
  public static String shortNameOf(DeclaredMethod method) {
    return shortNameOf(method.getDeclaringClass(), method.getName());
  }

  public static String shortNameOf(Method method) {
    return shortNameOf(method.getDeclaringClass(), method.getName());
  }

  private static String shortNameOf(WrappedClass declaringClass, String methodName) {
    var declaringClassName = declaringClass.getFullyQualifiedName();
    final var lastDotIndex = declaringClassName.lastIndexOf('.');

    if (lastDotIndex > 0) {
      declaringClassName = declaringClassName.substring(lastDotIndex + 1);
    }

    return "`" + declaringClassName + "." + methodName + "`";
  }
}
