package org.sonarcrypto.scanner;

import boomerang.scope.DataFlowScope;
import boomerang.scope.sootup.BoomerangPreInterceptor;
import com.google.common.base.Stopwatch;
import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisScope;
import de.fraunhofer.iem.framework.FrameworkSetup;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import sootup.callgraph.CallGraph;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.callgraph.RapidTypeAnalysisAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootClass;
import sootup.core.model.SootClassMember;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.transform.BodyInterceptor;
import sootup.core.views.View;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;

public class CryptoAnalysisSootUpSetup extends FrameworkSetup {

    private View view;

    public CryptoAnalysisSootUpSetup(
            String applicationPath,
            ScannerSettings.CallGraphAlgorithm algorithm,
            DataFlowScope dataFlowScope) {
        super(applicationPath, algorithm, dataFlowScope);
    }

    @Override
    public void initializeFramework() {
        LOGGER.info("Setting up SootUp...");
        Stopwatch watch = Stopwatch.createStarted();

        List<BodyInterceptor> interceptors = List.of(new BoomerangPreInterceptor());
        AnalysisInputLocation inputLocation =
                new JavaClassPathAnalysisInputLocation(
                        applicationPath, SourceType.Application, interceptors);

        view = new JavaView(inputLocation);

        watch.stop();
        LOGGER.info("SootUp setup done in {}", watch);
    }

    @Override
    public CryptoAnalysisScope createFrameworkScope() {
        Collection<SootMethod> entryPoints = new HashSet<>();
        view.getClasses()
                .filter(SootClass::isApplicationClass)
                .forEach(
                        c -> {
                            for (SootMethod method : c.getMethods()) {
                                if (method.hasBody()) {
                                    entryPoints.add(method);
                                }
                            }
                        });

        CallGraphAlgorithm algorithm = getCallGraphAlgorithm(view);
        CallGraph callGraph =
                algorithm.initialize(
                        entryPoints.stream().map(SootClassMember::getSignature).toList());

        return new CryptoAnalysisSootUpScope(view, callGraph, entryPoints, dataFlowScope);
    }

    private CallGraphAlgorithm getCallGraphAlgorithm(View view) {
        switch (callGraphAlgorithm) {
            case CHA -> {
                return new ClassHierarchyAnalysisAlgorithm(view);
            }
            case RTA -> {
                return new RapidTypeAnalysisAlgorithm(view);
            }
            default ->
                    throw new RuntimeException(
                            "SootUp does not support call graph algorithm " + callGraphAlgorithm);
        }
    }
}
