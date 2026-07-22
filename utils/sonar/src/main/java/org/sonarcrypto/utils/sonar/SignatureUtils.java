package org.sonarcrypto.utils.sonar;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SignatureUtils {

  private SignatureUtils() {
    // Utility class
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
