
/********************************************************************************
 * Copyright (c) 2017 Fraunhofer IEM, Paderborn, Germany
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.sonarcrypto.scanner;


import boomerang.scope.DataFlowScope;
import boomerang.scope.FrameworkScope;
import boomerang.scope.sootup.SootUpCallGraph;
import boomerang.scope.sootup.SootUpFrameworkScope;
import de.fraunhofer.iem.cryptoanalysis.handler.FrameworkHandler;
import de.fraunhofer.iem.cryptoanalysis.handler.SootUpFrameworkHandler;
import java.util.Collection;

import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisScope;
import org.jspecify.annotations.NonNull;
import sootup.callgraph.CallGraph;
import sootup.core.views.View;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class CommonCryptoAnalysisSootUpScope implements CryptoAnalysisScope, FrameworkScope {
	
	public static final String CONSTRUCTOR_NAME = "<init>";
	public static final String STATIC_INITIALIZER_NAME = "<clinit>";
	
	protected final View view;
	protected final boomerang.scope.CallGraph sootUpCallGraph;
	protected DataFlowScope dataflowScope;
	
	public CommonCryptoAnalysisSootUpScope(
		@NonNull View view,
		sootup.callgraph.@NonNull CallGraph callGraph,
		@NonNull Collection<JavaSootMethod> entryPoints,
		@NonNull DataFlowScope dataFlowScope) {
		this.view = view;
		this.sootUpCallGraph = new SootUpCallGraph(view, callGraph, entryPoints);
		this.dataflowScope = dataFlowScope;
	}
	
	@Override
	public boomerang.scope.CallGraph getCallGraph() {
		return sootUpCallGraph;
	}
	
	@Override
	public DataFlowScope getDataFlowScope() {
		return dataflowScope;
	}

    @Override
    public FrameworkScope asFrameworkScope() {
        return this;
    }

    @Override
    public FrameworkHandler getFrameworkHandler() {
        return new SootUpFrameworkHandler();
    }
}

