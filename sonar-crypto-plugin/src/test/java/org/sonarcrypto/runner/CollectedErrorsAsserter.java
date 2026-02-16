package org.sonarcrypto.runner;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.util.Collection;
import java.util.Set;
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
}
