package org.sonarcrypto.utils.test.asserts;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.util.*;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.ObjectAssert;
import org.jspecify.annotations.NullMarked;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CcErrorsAssert {
  private static final Logger LOGGER = LoggerFactory.getLogger(CcErrorsAssert.class);

  /**
   * Example:
   *
   * <pre><code>
   * assertOnFilteredCcErrorsThat(
   *     collectedErrors,
   *     "com.example.crypto.SomeClass",
   *     "byte[] encryptWithDES(byte[])"
   * )
   * .hasSize(5)
   * .anySatisfy(error -> assertThat(error).isInstanceOf(ConstraintError.class))
   * .noneSatisfy(error -> assertThat(error).isInstanceOf(TypestateError.class))
   * .extracting(AbstractError::getLineNumber)
   * .containsExactlyInAnyOrder(31, 34, 34, 34, 35);
   * </code></pre>
   */
  public static AbstractCollectionAssert<
          ?, Collection<? extends AbstractError>, AbstractError, ObjectAssert<AbstractError>>
      assertOnFilteredCcErrorsThat(
          Table<WrappedClass, Method, Set<AbstractError>> collectedErrors,
          String className,
          String methodSubSignature) {
    return assertThat(
        collectedErrors.cellSet().stream()
            .filter(
                cell ->
                    cell.getRowKey().getFullyQualifiedName().equals(className)
                        && cell.getColumnKey().getSubSignature().equals(methodSubSignature))
            .map(Table.Cell::getValue)
            .filter(not(Objects::isNull))
            .findFirst()
            .orElse(Set.of()));
  }

  public static void assertEquals(
      String message,
      Table<WrappedClass, Method, Set<AbstractError>> collectedErrors1,
      Table<WrappedClass, Method, Set<AbstractError>> collectedErrors2) {
    final var errorMap = makeComparable(collectedErrors1);
    final var otherErrorMap = makeComparable(collectedErrors2);
    final var diffMap = new HashMap<String, Diff>();

    for (final var entry : errorMap.entrySet()) {
      final var methodSignature = entry.getKey();
      final var errors = entry.getValue();

      final var otherErrors = otherErrorMap.remove(methodSignature);

      if (otherErrors == null) {
        diffMap.put(methodSignature, new Diff(Set.of(), new HashSet<>(errors)));
        continue;
      }

      errors.removeAll(otherErrors);
      otherErrors.removeAll(errors);

      if (errors.size() != otherErrors.size()) {
        diffMap.put(methodSignature, new Diff(otherErrors, errors));
      }
    }

    for (final var otherEntry : otherErrorMap.entrySet()) {
      final var otherMethodSignature = otherEntry.getKey();
      final var otherErrors = otherEntry.getValue();

      diffMap.put(otherMethodSignature, new Diff(new HashSet<>(otherErrors), Set.of()));
    }

    final var stringBuilder = new StringBuilder();

    if (!diffMap.isEmpty()) {

      stringBuilder.append("#### Diff Results #########################################");
      stringBuilder.append(System.lineSeparator());

      var printSeparator = false;

      for (final var entry : diffMap.entrySet()) {
        final var methodSignature = entry.getKey();
        final var diff = entry.getValue();

        if (printSeparator) {
          stringBuilder.append("-----------------------------------------------------------");
          stringBuilder.append(System.lineSeparator());
        } else {
          printSeparator = true;
        }

        stringBuilder.append("Method: ").append(methodSignature);
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("    - Missing in first result:");

        if (diff.missingInFirst.isEmpty()) {
          stringBuilder.append("        <empty>");
        }

        for (final var error : diff.missingInFirst) {
          stringBuilder.append("        - ").append(error);
        }

        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("    - Missing in second result:");

        if (diff.missingInSecond.isEmpty()) {
          stringBuilder.append("        <empty>");
        }

        for (final var error : diff.missingInSecond) {
          stringBuilder.append("        - ").append(error);
        }
      }

      stringBuilder.append(System.lineSeparator());
      stringBuilder.append("###########################################################");
      stringBuilder.append(System.lineSeparator());
    }

    LOGGER.error(stringBuilder.toString());

    Assert.assertEquals(message, 0, diffMap.size());
  }

  private static HashMap<String, Set<AbstractError>> makeComparable(
      Table<WrappedClass, Method, Set<AbstractError>> collectedErrors) {
    final var cellSet = collectedErrors.cellSet();
    final var map = new HashMap<String, Set<AbstractError>>(cellSet.size());

    for (final var cell : cellSet) {
      final var className = cell.getRowKey().getFullyQualifiedName();
      final var methodSubSignature = cell.getColumnKey().getSubSignature();
      final var errors = cell.getValue();

      final var fqn = "<" + className + ": " + methodSubSignature + ">";

      if (map.putIfAbsent(fqn, errors) != null) {
        // Should never happen
        throw new RuntimeException("Fatal error: Duplicate key for map: " + fqn);
      }
    }

    return map;
  }

  public static class Diff {
    private final Set<AbstractError> missingInFirst;
    private final Set<AbstractError> missingInSecond;

    public Diff(Set<AbstractError> missingInFirst, Set<AbstractError> missingInSecond) {
      this.missingInFirst = missingInFirst;
      this.missingInSecond = missingInSecond;
    }
  }
}
