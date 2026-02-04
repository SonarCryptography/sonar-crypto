package org.sonarcrypto.scanner;


import boomerang.scope.CallGraph;
import boomerang.scope.DataFlowScope;
import boomerang.scope.FrameworkScope;
import de.fraunhofer.iem.cryptoanalysis.handler.FrameworkHandler;
import de.fraunhofer.iem.cryptoanalysis.handler.SootUpFrameworkHandler;
import java.util.Collection;

import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisScope;
import org.jspecify.annotations.NonNull;
import org.sonarcrypt.boomerang.scope.sootup.SootUpCallGraph;
import sootup.core.model.SootMethod;
import sootup.core.views.View;

public class CryptoAnalysisSootUpScope implements CryptoAnalysisScope, FrameworkScope {
	
	public static final String CONSTRUCTOR_NAME = "<init>";
	public static final String STATIC_INITIALIZER_NAME = "<clinit>";
	
	protected final View view;
	protected final CallGraph sootUpCallGraph;
	protected DataFlowScope dataflowScope;
	
	public CryptoAnalysisSootUpScope(
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

