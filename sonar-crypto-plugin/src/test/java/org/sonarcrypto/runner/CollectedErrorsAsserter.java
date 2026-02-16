package org.sonarcrypto.runner;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.util.*;
import org.jspecify.annotations.NullMarked;
import org.junit.Assert;

@NullMarked
public class CollectedErrorsAsserter {
  private final Table<WrappedClass, Method, Set<AbstractError>> collectedErrors;

  public CollectedErrorsAsserter(Table<WrappedClass, Method, Set<AbstractError>> collectedErrors) {
    this.collectedErrors = collectedErrors;
  }

  public void assertContainsAny(
      String className,
      String methodSubSignature,
      Collection<Class<? extends AbstractError>> errors) {
    final var hasAny =
        collectedErrors.cellSet().stream()
            .anyMatch(
                cell -> {
                  final var cellClassName = cell.getRowKey().getFullyQualifiedName();
                  final var cellMethodSubSignature = cell.getColumnKey().getSubSignature();
                  final var cellErrors = cell.getValue();

                  return className.equals(cellClassName)
                      && methodSubSignature.equals(cellMethodSubSignature)
                      && cellErrors.stream()
                          .anyMatch(
                              cellError ->
                                  errors.isEmpty()
                                      || errors.stream().anyMatch(it -> it.isInstance(cellError)));
                });

    Assert.assertTrue(hasAny);
  }

  public static void assertEquals(
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

    if (!diffMap.isEmpty()) {

      System.err.println("#### Diff Results #########################################");
      System.err.println();

      var printSeparator = false;

      for (final var entry : diffMap.entrySet()) {
        final var methodSignature = entry.getKey();
        final var diff = entry.getValue();

        if (printSeparator) {
          System.err.println("-----------------------------------------------------------");
        } else {
          printSeparator = true;
        }

        System.err.println("Method: " + methodSignature);
        System.err.println();
        System.err.println("    - Missing in first result:");

        if (diff.missingInFirst.isEmpty()) {
          System.err.println("        <empty>");
        }

        for (final var error : diff.missingInFirst) {
          System.err.println("        - " + error);
        }

        System.err.println();
        System.err.println("    - Missing in second result:");

        if (diff.missingInSecond.isEmpty()) {
          System.err.println("        <empty>");
        }

        for (final var error : diff.missingInSecond) {
          System.err.println("        - " + error);
        }
      }

      System.err.println();
      System.err.println("###########################################################");
    }

    Assert.assertEquals(
        "The collected errors of both executions are not equal!", 0, diffMap.size());
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
