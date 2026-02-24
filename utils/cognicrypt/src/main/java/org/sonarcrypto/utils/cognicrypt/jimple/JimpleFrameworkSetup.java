package org.sonarcrypto.utils.cognicrypt.jimple;

import boomerang.scope.DataFlowScope;
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
import sootup.core.model.SootClass;
import sootup.core.model.SootClassMember;
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

    AnalysisInputLocation inputLocation =
        new JimpleAnalysisInputLocation(
            Path.of(applicationPath), SourceType.Application, Collections.emptyList());
    view = new JimpleConvertingView(inputLocation);
    LOGGER.info("Got {} classes from Jimple input location.", view.getClasses().count());

    watch.stop();
    LOGGER.info("SootUp setup done in {}", watch);
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
