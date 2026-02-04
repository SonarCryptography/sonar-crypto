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
package org.sonarcrypt.boomerang.scope.sootup;

import boomerang.scope.CallGraph;
import boomerang.scope.DataFlowScope;
import boomerang.scope.FrameworkScope;
import java.util.Collection;
import org.jspecify.annotations.NonNull;
import sootup.core.model.SootMethod;
import sootup.core.views.View;

public class SootUpFrameworkScope implements FrameworkScope {

  public static final String CONSTRUCTOR_NAME = "<init>";
  public static final String STATIC_INITIALIZER_NAME = "<clinit>";

  protected final View view;
  protected final CallGraph sootUpCallGraph;
  protected DataFlowScope dataflowScope;

  public SootUpFrameworkScope(
      @NonNull View view,
      sootup.callgraph.@NonNull CallGraph callGraph,
      @NonNull Collection<SootMethod> entryPoints,
      @NonNull DataFlowScope dataFlowScope) {
    this.view = view;
    this.sootUpCallGraph = new SootUpCallGraph(view, callGraph, entryPoints);
    this.dataflowScope = dataFlowScope;
  }

  @Override
  public CallGraph getCallGraph() {
    return sootUpCallGraph;
  }

  @Override
  public DataFlowScope getDataFlowScope() {
    return dataflowScope;
  }
}
