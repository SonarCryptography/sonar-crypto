package org.sonarcrypto.utils.cognicrypt;

import boomerang.scope.sootup.BoomerangPreInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.utils.jimple.mapper.LineMapping;
import org.sonarcrypto.utils.jimple.mapper.LineMappingCollection;
import sootup.core.frontend.AbstractClassSource;
import sootup.core.frontend.OverridingBodySource;
import sootup.core.frontend.OverridingClassSource;
import sootup.core.frontend.ResolveException;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;
import sootup.core.model.*;
import sootup.core.types.ClassType;
import sootup.java.core.*;
import sootup.java.core.views.JavaView;

public class JimpleConvertingView extends JavaView {

  private static final Logger log = LoggerFactory.getLogger(JimpleConvertingView.class);

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

    private final Map<Integer, LineMapping> classMappings;
    private final Map<Integer, LineMapping> methodMappings;
    private final Map<Integer, LineMapping> fieldMappings;
    private final Map<Integer, LineMapping> statementMappings;

    private WrappingSootClassSource(OverridingClassSource classSource) {
      super(
          classSource.getAnalysisInputLocation(),
          classSource.getClassType(),
          classSource.getSourcePath());
      resolvedClass =
          classSource.buildClass(classSource.getAnalysisInputLocation().getSourceType());

      log.debug(
          "Wrapped class source of type {} resolved to class {}",
          classSource.getClass().getName(),
          resolvedClass.getName());

      LineMappingCollection loaded = readMapping(classSource.getSourcePath());
      if (loaded != null) {
        Map<Integer, LineMapping> classMap = new HashMap<>();
        Map<Integer, LineMapping> methodMap = new HashMap<>();
        Map<Integer, LineMapping> fieldMap = new HashMap<>();
        Map<Integer, LineMapping> stmtMap = new HashMap<>();
        for (LineMapping m : loaded.getMappings()) {
          switch (m.getElementType()) {
            case CLASS -> classMap.put(m.getJimpleLine(), m);
            case METHOD -> methodMap.put(m.getJimpleLine(), m);
            case FIELD -> fieldMap.put(m.getJimpleLine(), m);
            case STATEMENT -> stmtMap.put(m.getJimpleLine(), m);
          }
        }
        classMappings = Collections.unmodifiableMap(classMap);
        methodMappings = Collections.unmodifiableMap(methodMap);
        fieldMappings = Collections.unmodifiableMap(fieldMap);
        statementMappings = Collections.unmodifiableMap(stmtMap);
      } else {
        classMappings = Collections.emptyMap();
        methodMappings = Collections.emptyMap();
        fieldMappings = Collections.emptyMap();
        statementMappings = Collections.emptyMap();
      }
    }

    private @Nullable LineMappingCollection readMapping(@Nullable Path sourcePath) {
      if (sourcePath == null) {
        return null;
      }
      Path mappingFile = Path.of(sourcePath + ".map.json");
      if (!Files.exists(mappingFile)) {
        log.debug("No mapping file found at {}", mappingFile);
        return null;
      }
      try {
        return new ObjectMapper().readValue(mappingFile.toFile(), LineMappingCollection.class);
      } catch (IOException e) {
        log.warn("Failed to read mapping file {}: {}", mappingFile, e.getMessage());
        return null;
      }
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
                  final LocationReplacerInterceptor locationInterceptor =
                      new LocationReplacerInterceptor(statementMappings);
                  Body.BodyBuilder builder = Body.builder(m.getBody(), m.getModifiers());
                  locationInterceptor.interceptBody(builder, JimpleConvertingView.this);
                  interceptor.interceptBody(builder, JimpleConvertingView.this);
                  OverridingBodySource interceptedBodySource =
                      preInterceptedBodySource.withBody(builder.build());

                  LineMapping methodMapping = methodMappings.get(m.getPosition().getFirstLine());
                  Position methodPosition =
                      methodMapping != null
                          ? methodMapping.getSourcePosition().toSootUpPosition()
                          : m.getPosition();
                  return new JavaSootMethod(
                      interceptedBodySource,
                      m.getSignature(),
                      m.getModifiers(),
                      m.getExceptionSignatures(),
                      Collections.emptyList(),
                      methodPosition);
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
              f -> {
                LineMapping mapping = fieldMappings.get(f.getPosition().getFirstLine());
                Position position =
                    mapping != null
                        ? mapping.getSourcePosition().toSootUpPosition()
                        : f.getPosition();
                return new JavaSootField(
                    f.getSignature(), f.getModifiers(), Collections.emptyList(), position);
              })
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
      return classMappings.values().stream()
          .findAny()
          .map(m -> m.getSourcePosition().toSootUpPosition())
          .orElse(resolvedClass.getPosition());
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      WrappingSootClassSource that = (WrappingSootClassSource) o;
      return Objects.equals(resolvedClass, that.resolvedClass)
          && Objects.equals(classMappings, that.classMappings)
          && Objects.equals(methodMappings, that.methodMappings)
          && Objects.equals(fieldMappings, that.fieldMappings)
          && Objects.equals(statementMappings, that.statementMappings);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          super.hashCode(),
          resolvedClass,
          classMappings,
          methodMappings,
          fieldMappings,
          statementMappings);
    }
  }
}
