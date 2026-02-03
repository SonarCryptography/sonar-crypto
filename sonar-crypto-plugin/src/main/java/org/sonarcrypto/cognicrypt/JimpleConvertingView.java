package org.sonarcrypto.cognicrypt;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import sootup.core.cache.provider.ClassCacheProvider;
import sootup.core.frontend.AbstractClassSource;
import sootup.core.frontend.ResolveException;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.*;
import sootup.core.types.ClassType;
import sootup.java.core.*;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

public class JimpleConvertingView extends JavaView {
  protected JimpleConvertingView(
      @NonNull List<AnalysisInputLocation> inputLocations,
      @NonNull ClassCacheProvider cacheProvider,
      @NonNull JavaIdentifierFactory idf) {
    super(inputLocations, cacheProvider, idf);
  }

  public JimpleConvertingView(
      @NonNull List<AnalysisInputLocation> inputLocations,
      @NonNull ClassCacheProvider cacheProvider) {
    super(inputLocations, cacheProvider);
  }

  public JimpleConvertingView(@NonNull List<AnalysisInputLocation> inputLocations) {
    super(inputLocations);
  }

  public JimpleConvertingView(@NonNull AnalysisInputLocation inputLocation) {
    super(inputLocation);
  }

  @Override
  protected synchronized @NonNull JavaSootClass buildClassFrom(AbstractClassSource classSource) {
    ClassType classType = classSource.getClassType();
    JavaSootClass theClass;
    if (cache.hasClass(classType)) {
      theClass = (JavaSootClass) cache.getClass(classType);
    } else {
      var sc = classSource.buildClass(classSource.getAnalysisInputLocation().getSourceType());

      // Convert SootClass to JavaSootClass wrapper
      if (sc instanceof JavaSootClass) {
        theClass = (JavaSootClass) sc;
      } else {
        // Wrap plain SootClass as JavaSootClass with default values
        theClass = wrapAsJavaSootClass(sc, classSource);
      }

      cache.putClass(classType, theClass);
    }
    return theClass;
  }

  private JavaSootClass wrapAsJavaSootClass(SootClass sootClass, AbstractClassSource classSource) {
    // Create a JavaSootClassSource with default Java-specific metadata

    var sc =
        new WrappingSootClassSource(
            sootClass,
            (JimpleAnalysisInputLocation) classSource.getAnalysisInputLocation(),
            sootClass.getType());

    return sc.buildClass(classSource.getAnalysisInputLocation().getSourceType());
  }

  private static class WrappingSootClassSource extends JavaSootClassSource {

    private final SootClass resolvedClass;

    private WrappingSootClassSource(
        SootClass sootClass, JimpleAnalysisInputLocation srcNamespace, ClassType classSignature) {
      super(srcNamespace, classSignature, Path.of(""));
      this.resolvedClass = sootClass;
    }

    @Override
    protected Iterable<AnnotationUsage> resolveAnnotations() {
      return Collections.emptyList();
    }

    @Override
    public @NonNull Collection<? extends SootMethod> resolveMethods() throws ResolveException {
      return resolvedClass.getMethods().stream()
          .map(
              m ->
                  new JavaSootMethod(
                      m.getBodySource(),
                      m.getSignature(),
                      m.getModifiers(),
                      m.getExceptionSignatures(),
                      Collections.emptyList(),
                      m.getPosition()))
          .collect(Collectors.toSet());
    }

    @Override
    public @NonNull Collection<? extends SootField> resolveFields() throws ResolveException {
      return resolvedClass.getFields().stream()
          .map(
              f ->
                  new JavaSootField(
                      f.getSignature(), f.getModifiers(), Collections.emptyList(), f.getPosition()))
          .collect(Collectors.toSet());
    }

    @Override
    public @NonNull Set<ClassModifier> resolveModifiers() {
      return resolvedClass.getModifiers();
    }

    @Override
    public @NonNull Set<? extends ClassType> resolveInterfaces() {
      return resolvedClass.getInterfaces();
    }

    @Override
    public @NonNull Optional<? extends ClassType> resolveSuperclass() {
      return resolvedClass.getSuperclass();
    }

    @Override
    public @NonNull Optional<? extends ClassType> resolveOuterClass() {
      return resolvedClass.getOuterClass();
    }

    @Override
    public @NonNull Position resolvePosition() {
      return resolvedClass.getPosition();
    }
  }
}
