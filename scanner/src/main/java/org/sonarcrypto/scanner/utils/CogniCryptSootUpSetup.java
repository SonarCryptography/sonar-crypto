/*
 ********************************************************************************
 * Copyright (c) 2017 Fraunhofer IEM, Paderborn, Germany
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************
 */
package org.sonarcrypto.scanner.utils;

import boomerang.scope.DataFlowScope;
import boomerang.scope.sootup.BoomerangPreInterceptor;
import com.google.common.base.Stopwatch;
import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisScope;
import de.fraunhofer.iem.framework.FrameworkSetup;
import de.fraunhofer.iem.scanner.ScannerSettings;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import sootup.callgraph.CallGraph;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.callgraph.RapidTypeAnalysisAlgorithm;
import sootup.core.model.SootClass;
import sootup.core.model.SootClassMember;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.views.View;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;
import sootup.jimple.frontend.JimpleView;

@NullMarked
public class CogniCryptSootUpSetup extends FrameworkSetup {
    /**
     * Input location type
     */
    public enum InputLocationType {
        /**
         * Java class path.
         * <p>
         * Applies the {@link BoomerangPreInterceptor} when loading the code.
         */
        JAVA_CLASS_PATH,
        
        /**
         * Jimple files.
         * <p>
         * Does NOT apply the {@link BoomerangPreInterceptor} due to certain limitations.
         * Expects the Jimple code to contain the required changes for Boomerang.
         */
        JIMPLE
    }
    
    private final InputLocationType inputLocationType;
    
    private @Nullable View view;
    
    public CogniCryptSootUpSetup(
        InputLocationType inputLocationType,
        String applicationPath,
        ScannerSettings.CallGraphAlgorithm algorithm,
        DataFlowScope dataFlowScope
    ) {
        super(applicationPath, algorithm, dataFlowScope);
        
        this.inputLocationType = inputLocationType;
    }

    @Override
    public void initializeFramework() {
        LOGGER.info("Setting up SootUp...");
        Stopwatch watch = Stopwatch.createStarted();

        view = switch(inputLocationType) {
            case JAVA_CLASS_PATH -> new JavaView(new JavaClassPathAnalysisInputLocation(
                applicationPath,
                SourceType.Application,
                List.of(new BoomerangPreInterceptor())
            ));
            
            case JIMPLE -> new JimpleView(new JimpleAnalysisInputLocation(
                Path.of(applicationPath),
                SourceType.Application,
                List.of()
            ));
        };

        watch.stop();
        LOGGER.info("SootUp setup done in {}", watch);
    }

    @Override
    public CryptoAnalysisScope createFrameworkScope() {
        Collection<SootMethod> entryPoints = new HashSet<>();
		
        //noinspection DataFlowIssue The `view` is expected to be initialized.
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
        CallGraph callGraph = algorithm.initialize(
            entryPoints.stream().map(SootClassMember::getSignature).toList()
        );

        return new CogniCryptSootUpScope(view, callGraph, entryPoints, dataFlowScope);
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
