/*
 * ***************************************************************************** 
 * Copyright (c) 2018 Fraunhofer IEM, Paderborn, Germany
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 *   Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package org.sonarcrypt.boomerang.scope.sootup.jimple;

import boomerang.scope.Method;
import boomerang.scope.Type;
import boomerang.scope.WrappedClass;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;

public class JimpleUpWrappedClass implements WrappedClass {

  private final ClassType delegate;
  private final View view;
  private Collection<Method> methodsCache;

  public JimpleUpWrappedClass(ClassType delegate, View view) {
    this.view = view;
    this.delegate = delegate;
  }

  public ClassType getDelegate() {
    return delegate;
  }

  @Override
  public Collection<Method> getMethods() {
    if (methodsCache == null) {
      methodsCache = new HashSet<>();

      Optional<? extends SootClass> sootClass = view.getClass(delegate);

      if (sootClass.isPresent()) {
        for (SootMethod method : sootClass.get().getMethods()) {
          if (method.hasBody()) {
            methodsCache.add(JimpleUpMethod.of(method, view));
          }
        }
      }
    }
    return methodsCache;
  }

  @Override
  public boolean hasSuperclass() {
    Optional<? extends SootClass> sootClass = view.getClass(delegate);

    return sootClass.map(SootClass::hasSuperclass).orElse(false);
  }

  @Override
  public WrappedClass getSuperclass() {
    Optional<? extends SootClass> sootClass = view.getClass(delegate);
    if (sootClass.isPresent() && sootClass.get().hasSuperclass()) {

      Optional<? extends ClassType> superClassType = sootClass.get().getSuperclass();
      if (superClassType.isEmpty()) {
        throw new RuntimeException("Super class type of " + superClassType + " is not present");
      }

      return new JimpleUpWrappedClass(superClassType.get(), view);
    }

    throw new RuntimeException("Class " + delegate + " has no super class");
  }

  @Override
  public Type getType() {
    return new JimpleUpType(delegate, view);
  }

  @Override
  public boolean isApplicationClass() {
    Optional<? extends SootClass> sootClass = view.getClass(delegate);
    return sootClass.map(SootClass::isApplicationClass).orElse(false);
  }

  @Override
  public String getFullyQualifiedName() {
    return delegate.getFullyQualifiedName();
  }

  @Override
  public boolean isDefined() {
    Optional<? extends SootClass> sootClass = view.getClass(delegate);
    return sootClass.isPresent();
  }

  @Override
  public boolean isPhantom() {
    Optional<? extends SootClass> sootClass = view.getClass(delegate);
    return sootClass.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JimpleUpWrappedClass that = (JimpleUpWrappedClass) o;
    return Objects.equals(delegate, that.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
