package org.sonarcrypto.cognicrypt;

import boomerang.scope.sootup.BoomerangPreInterceptor;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import sootup.core.frontend.AbstractClassSource;
import sootup.core.frontend.OverridingBodySource;
import sootup.core.frontend.ResolveException;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.*;
import sootup.core.types.ClassType;
import sootup.java.core.*;
import sootup.java.core.views.JavaView;

public class JimpleConvertingView extends JavaView {

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
    WrappingSootClassSource wrappingSootClassSource =
        new WrappingSootClassSource(
            sootClass, classSource.getAnalysisInputLocation(), sootClass.getType());

    return wrappingSootClassSource.buildClass(
        classSource.getAnalysisInputLocation().getSourceType());
  }

  private class WrappingSootClassSource extends JavaSootClassSource {
    private final SootClass resolvedClass;

    private WrappingSootClassSource(
        SootClass sootClass, AnalysisInputLocation srcNamespace, ClassType classSignature) {
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
              m -> {
                if (m.getBodySource() instanceof OverridingBodySource preInterceptedBodySource) {
                  final BoomerangPreInterceptor interceptor = new BoomerangPreInterceptor();
                  Body.BodyBuilder builder = Body.builder(m.getBody(), m.getModifiers());
                  interceptor.interceptBody(builder, JimpleConvertingView.this);
                  OverridingBodySource interceptedBodySource =
                      preInterceptedBodySource.withBody(builder.build());
                  return new JavaSootMethod(
                      interceptedBodySource,
                      m.getSignature(),
                      m.getModifiers(),
                      m.getExceptionSignatures(),
                      Collections.emptyList(),
                      m.getPosition());
                } else {
                  throw new RuntimeException("");
                }
              })
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
