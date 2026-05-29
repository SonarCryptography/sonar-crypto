package org.sonarcrypto.downloadrules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DownloadRulesetWithDependenciesMojoTest {

  @TempDir Path tempDir;

  @Test
  void resolveRulesetFolderNames() throws Exception {
    var mojo = new DownloadRulesetWithDependenciesMojo();
    setField(mojo, "rulesetFolderMappings", List.of("g:a=alpha", "x:y=beta"));

    @SuppressWarnings("unchecked")
    var result = (Map<String, String>) invoke(mojo, "resolveRulesetFolderNames");

    assertThat(result).containsEntry("g:a", "alpha").containsEntry("x:y", "beta");
  }

  @Test
  void resolveRulesetFolderNames_rejects_invalid_entries() throws Exception {
    var mojo = new DownloadRulesetWithDependenciesMojo();
    setField(mojo, "rulesetFolderMappings", List.of("broken"));

    assertThatThrownBy(() -> invoke(mojo, "resolveRulesetFolderNames"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid rulesetFolderMappings entry");
  }

  @Test
  void deleteGeneratedFiles_and_copyDependencyJar() throws Exception {
    var rulesetDirectory = Files.createDirectory(tempDir.resolve("rules"));
    Files.writeString(rulesetDirectory.resolve("old.zip"), "zip");
    Files.writeString(rulesetDirectory.resolve("old.jar"), "jar");
    var keptFile = Files.writeString(rulesetDirectory.resolve("keep.txt"), "keep");

    invokeStatic("deleteGeneratedFiles", new Class<?>[] {Path.class}, rulesetDirectory);

    assertThat(rulesetDirectory.resolve("old.zip")).doesNotExist();
    assertThat(rulesetDirectory.resolve("old.jar")).doesNotExist();
    assertThat(keptFile).exists();

    var sourceJar =
        Files.writeString(tempDir.resolve("artifact-source.jar"), "jar-content").toFile();
    Files.writeString(rulesetDirectory.resolve("artifact.jar"), "existing");
    Artifact artifact = mock(Artifact.class);
    when(artifact.getArtifactId()).thenReturn("artifact");
    when(artifact.getVersion()).thenReturn("1.0");
    when(artifact.getFile()).thenReturn(sourceJar);

    var copiedPath =
        (Path)
            invokeStatic(
                "copyDependencyJar",
                new Class<?>[] {Artifact.class, Path.class},
                artifact,
                rulesetDirectory);

    assertThat(copiedPath.getFileName().toString()).isEqualTo("artifact-1.0.jar");
    assertThat(Files.readString(copiedPath)).isEqualTo("jar-content");
  }

  @Test
  void shouldSkipDependency() throws Exception {
    var compileDependency = dependency(null, null);
    var testDependency = dependency("test", null);
    var providedDependency = dependency("provided", null);
    var systemDependency = dependency("system", null);
    var importDependency = dependency("import", null);
    var optionalDependency = dependency(null, "true");

    assertThat(
            (boolean)
                invokeStatic(
                    "shouldSkipDependency", new Class<?>[] {Dependency.class}, compileDependency))
        .isFalse();
    assertThat(
            (boolean)
                invokeStatic(
                    "shouldSkipDependency", new Class<?>[] {Dependency.class}, testDependency))
        .isTrue();
    assertThat(
            (boolean)
                invokeStatic(
                    "shouldSkipDependency", new Class<?>[] {Dependency.class}, providedDependency))
        .isTrue();
    assertThat(
            (boolean)
                invokeStatic(
                    "shouldSkipDependency", new Class<?>[] {Dependency.class}, systemDependency))
        .isTrue();
    assertThat(
            (boolean)
                invokeStatic(
                    "shouldSkipDependency", new Class<?>[] {Dependency.class}, importDependency))
        .isTrue();
    assertThat(
            (boolean)
                invokeStatic(
                    "shouldSkipDependency", new Class<?>[] {Dependency.class}, optionalDependency))
        .isTrue();
  }

  private static Dependency dependency(String scope, String optional) {
    var dependency = new Dependency();
    dependency.setGroupId("g");
    dependency.setArtifactId("a");
    dependency.setScope(scope);
    dependency.setOptional(optional);
    return dependency;
  }

  private static Object invoke(Object target, String methodName) throws Exception {
    Method method = target.getClass().getDeclaredMethod(methodName);
    method.setAccessible(true);
    try {
      return method.invoke(target);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception cause) {
        throw cause;
      }
      throw e;
    }
  }

  private static Object invokeStatic(String methodName, Class<?>[] parameterTypes, Object... args)
      throws Exception {
    Method method =
        DownloadRulesetWithDependenciesMojo.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    try {
      return method.invoke(null, args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Exception cause) {
        throw cause;
      }
      throw e;
    }
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    var field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
