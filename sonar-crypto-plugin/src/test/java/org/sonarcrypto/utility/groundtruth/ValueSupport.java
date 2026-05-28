package org.sonarcrypto.utility.groundtruth;

import static org.sonarcrypto.utils.cognicrypt.boomerang.SignatureUtils.shortNameOf;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.ccerror.causes.*;

@NullMarked
public class ValueSupport {
  public static <T extends Cause> @Nullable String getValue(T instance) {
    if (instance instanceof ForbiddenMethodCause cause) {
      final var forbiddenMethod = cause.getForbiddenMethod();
      return shortNameOf(
          forbiddenMethod.getDeclaringClass().getFullyQualifiedName(), forbiddenMethod.getName());
    }
    if (instance instanceof ForbiddenTypeCause cause)
      return StringEscapeUtils.escapeJava(cause.getDisallowedType());
    if (instance instanceof IncompleteOperationCause cause) {
      final var incompleteObject = cause.getIncompleteObject();
      if (incompleteObject instanceof IncompleteOperationCause.TypedIncompleteObject tio)
        return tio.getClassName();
      return null;
    }
    if (instance instanceof InvalidValueCause cause) {
      final var actualValues = cause.getActualValues();
      if (actualValues.isEmpty()) return null;
      return actualValues.get(0);
    }
    if (instance instanceof UncaughtExceptionCause cause)
      return cause.getUncaughtException().getFullyQualifiedName();
    if (instance instanceof UnexpectedCallCause cause) {
      final var unexpectedMethod = cause.getUnexpectedMethod();
      return shortNameOf(
          unexpectedMethod.getDeclaringClass().getFullyQualifiedName(), unexpectedMethod.getName());
    }
    return null;
  }
}
