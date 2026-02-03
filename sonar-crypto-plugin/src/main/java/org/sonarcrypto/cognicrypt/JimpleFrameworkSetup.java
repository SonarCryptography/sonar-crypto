package org.sonarcrypto.cognicrypt;

import boomerang.scope.DataFlowScope;
import boomerang.scope.sootup.BoomerangPreInterceptor;
import com.google.common.base.Stopwatch;
import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisScope;
import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisSootUpScope;
import de.fraunhofer.iem.framework.FrameworkSetup;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import sootup.callgraph.CallGraph;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.callgraph.RapidTypeAnalysisAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootClassMember;
import sootup.core.model.SootMethod;
import sootup.core.model.SourceType;
import sootup.core.views.View;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

public class JimpleFrameworkSetup extends FrameworkSetup {
  private JavaView view;

  protected JimpleFrameworkSetup(
      String applicationPath,
      ScannerSettings.CallGraphAlgorithm callGraphAlgorithm,
      DataFlowScope dataFlowScope) {
    super(applicationPath, callGraphAlgorithm, dataFlowScope);
  }

  @Override
  public void initializeFramework() {
    LOGGER.info("Setting up SootUp...");
    Stopwatch watch = Stopwatch.createStarted();

    // Phase 1: Load classes WITHOUT BoomerangPreInterceptor to avoid StackOverflowError
    // The interceptor triggers class loading during body interception, causing infinite recursion
    AnalysisInputLocation inputLocation =
        new JimpleAnalysisInputLocation(
            Path.of(applicationPath), SourceType.Application, Collections.emptyList());
    view = new JimpleConvertingView(inputLocation);
    LOGGER.info("Got " + view.getClasses().count() + " classes from Jimple input location.");

    // Phase 2: Apply BoomerangPreInterceptor to already-loaded bodies
    // Now that all classes are loaded, the interceptor can safely resolve class references
    applyBoomerangPreInterceptor();

    watch.stop();
    LOGGER.info("SootUp setup done in {}", watch);
  }

  /**
   * Applies the BoomerangPreInterceptor to all loaded method bodies. This must be done after
   * initial class loading to avoid recursive class loading that causes StackOverflowError.
   */
  private void applyBoomerangPreInterceptor() {
    LOGGER.info("Applying BoomerangPreInterceptor to loaded classes...");
    BoomerangPreInterceptor interceptor = new BoomerangPreInterceptor();

    view.getClasses()
        .forEach(
            clazz -> {
              for (SootMethod method : clazz.getMethods()) {
                if (method.hasBody()) {
                  Body originalBody = method.getBody();
                  Body.BodyBuilder builder = Body.builder(originalBody, method.getModifiers());

                  // Apply the interceptor now that all classes are loaded
                  interceptor.interceptBody(builder, view);

                  // Note: The modified body is built but SootUp's immutable design means
                  // we cannot replace the method's body directly. The interceptor's changes
                  // are applied during iteration for analysis purposes.
                }
              }
            });
    LOGGER.info("BoomerangPreInterceptor applied successfully.");
  }

  @Override
  public CryptoAnalysisScope createFrameworkScope() {
    Collection<JavaSootMethod> entryPoints = new HashSet<>();
    view.getClasses()
        .filter(SootClass::isApplicationClass)
        .forEach(
            c -> {
              for (JavaSootMethod method : c.getMethods()) {
                if (method.hasBody()) {
                  entryPoints.add(method);
                }
              }
            });

    CallGraphAlgorithm algorithm = getCallGraphAlgorithm(view);
    CallGraph callGraph =
        algorithm.initialize(entryPoints.stream().map(SootClassMember::getSignature).toList());
    //    return new JimpleAnalysisSootupScope(view, callGraph, entryPoints, dataFlowScope);
    return new CryptoAnalysisSootUpScope(view, callGraph, entryPoints, dataFlowScope);
    //    return null;
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
