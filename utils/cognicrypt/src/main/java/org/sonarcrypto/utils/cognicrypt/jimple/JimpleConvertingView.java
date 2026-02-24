package org.sonarcrypto.utils.cognicrypt.jimple;

import boomerang.scope.sootup.BoomerangPreInterceptor;
import java.util.*;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import sootup.core.frontend.AbstractClassSource;
import sootup.core.frontend.OverridingBodySource;
import sootup.core.frontend.OverridingClassSource;
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
      if (classSource instanceof JavaSootClassSource) {
        theClass = (JavaSootClass) classSource.buildClass(SourceType.Application);
      } else if (classSource instanceof OverridingClassSource) {
        WrappingSootClassSource wrappingSootClassSource =
            new WrappingSootClassSource((OverridingClassSource) classSource);
        theClass = wrappingSootClassSource.buildClass(SourceType.Application);
      } else {
        throw new RuntimeException(
            "Unsupported class source type: " + classSource.getClass().getName());
      }
      cache.putClass(classType, theClass);
    }
    return theClass;
  }

  @Override
  protected @NonNull Optional<JavaSootClassSource> getClassSource(@NonNull ClassType type) {
    return inputLocations.parallelStream()
        .map(location -> location.getClassSource(type, this))
        .filter(Optional::isPresent)
        // like javas behaviour: if multiple matching Classes(ClassTypes) are found on the
        // classpath the first is returned (see splitpackage)
        .limit(1)
        .map(Optional::get)
        .map(
            classSource -> {
              if (classSource instanceof JavaSootClassSource) {
                return (JavaSootClassSource) classSource;
              } else if (classSource instanceof OverridingClassSource) {
                return new WrappingSootClassSource((OverridingClassSource) classSource);
              } else {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findAny();
  }

  private class WrappingSootClassSource extends JavaSootClassSource {
    private final SootClass resolvedClass;

    private WrappingSootClassSource(OverridingClassSource classSource) {
      super(
          classSource.getAnalysisInputLocation(),
          classSource.getClassType(),
          classSource.getSourcePath());
      resolvedClass =
          classSource.buildClass(classSource.getAnalysisInputLocation().getSourceType());
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
                  throw new RuntimeException(
                      "Wrapped body source is not an OverridingBodySource, cannot apply BoomerangPreInterceptor.");
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
