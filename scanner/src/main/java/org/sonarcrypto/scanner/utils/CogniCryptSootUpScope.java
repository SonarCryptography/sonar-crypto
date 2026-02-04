package org.sonarcrypto.scanner.utils;


import boomerang.scope.CallGraph;
import boomerang.scope.DataFlowScope;
import boomerang.scope.FrameworkScope;
import de.fraunhofer.iem.cryptoanalysis.handler.FrameworkHandler;
import java.util.Collection;

import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisScope;
import org.jspecify.annotations.NonNull;
import org.sonarcrypto.scanner.boomerang.scope.sootup.SootUpCallGraph;
import org.sonarcrypto.scanner.boomerang.scope.sootup.SootUpFrameworkHandler;
import sootup.core.model.SootMethod;
import sootup.core.views.View;

public class CogniCryptSootUpScope implements CryptoAnalysisScope, FrameworkScope {
	
	protected final View view;
	protected final CallGraph sootUpCallGraph;
	protected DataFlowScope dataflowScope;
	
	public CogniCryptSootUpScope(
		@NonNull View view,
		sootup.callgraph.@NonNull CallGraph callGraph,
		@NonNull Collection<SootMethod> entryPoints,
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

