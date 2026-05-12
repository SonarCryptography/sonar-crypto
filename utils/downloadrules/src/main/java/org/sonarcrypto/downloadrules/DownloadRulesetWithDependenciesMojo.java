package org.sonarcrypto.downloadrules;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Mojo(
    name = "download-ruleset-with-dependencies",
    defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.TEST)
public class DownloadRulesetWithDependenciesMojo extends AbstractMojo {

  @Component private @Nullable RepositorySystem repositorySystem;

  @Component private @Nullable ProjectBuilder projectBuilder;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private @Nullable MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private @Nullable MavenSession session;

  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
  private @Nullable RepositorySystemSession repositorySystemSession;

  @Parameter(
      defaultValue = "${project.remoteProjectRepositories}",
      readonly = true,
      required = true)
  private @Nullable List<RemoteRepository> remoteProjectRepositories;

  @Parameter(
      property = "downloadrules.outputDirectory",
      defaultValue = "${project.basedir}/src/main/resources/crysl_rules",
      required = true)
  private @Nullable File outputDirectory;

  @Parameter private @Nullable List<String> rulesetFolderMappings;

  @Override
  public void execute() throws MojoExecutionException {
    final var resolvedArtifacts =
        requireProject().getArtifacts().stream()
            .sorted(Comparator.comparing(Artifact::getId))
            .toList();
    final var rulesetFolderNames = resolveRulesetFolderNames();

    final var rulesets =
        requireProject().getDependencies().stream()
            .map(this::withResolvedVersion)
            .filter(DownloadRulesetWithDependenciesMojo::isRulesetPomDependency)
            .map(dependency -> toRulesetSpec(dependency, rulesetFolderNames))
            .sorted(Comparator.comparing(RulesetSpec::coordinates))
            .toList();

    for (final var ruleset : rulesets) {
      syncRuleset(ruleset, resolveDependencyTrailRoots(ruleset), resolvedArtifacts);
    }
  }

  private void syncRuleset(
      RulesetSpec ruleset, List<String> dependencyTrailRoots, List<Artifact> resolvedArtifacts)
      throws MojoExecutionException {
    final var rulesetDirectory = requireOutputDirectory().toPath().resolve(ruleset.folderName());

    try {
      Files.createDirectories(rulesetDirectory);
      deleteGeneratedFiles(rulesetDirectory);

      final var rulesetZip = resolveRulesetZip(ruleset);

      final var copiedZip = copyRulesetZip(ruleset, rulesetZip, rulesetDirectory);
      getLog().info("Copied ruleset ZIP to " + copiedZip);

      final var dependencyJars =
          resolvedArtifacts.stream()
              .filter(artifact -> ruleset.isTransitiveLibraryJar(artifact, dependencyTrailRoots))
              .toList();

      if (dependencyJars.isEmpty()) {
        getLog().info("No transitive library JARs found for " + ruleset.coordinates());
        return;
      }

      for (final var dependencyJar : dependencyJars) {
        final var copiedJar = copyDependencyJar(dependencyJar, rulesetDirectory);
        getLog().info("Copied dependency JAR to " + copiedJar);
      }
    } catch (IOException e) {
      throw new MojoExecutionException(
          "Failed to prepare ruleset resources for " + ruleset.coordinates(), e);
    }
  }

  private static void deleteGeneratedFiles(Path rulesetDirectory) throws IOException {
    try (var files = Files.list(rulesetDirectory)) {
      for (final var file : files.toList()) {
        final var fileName = file.getFileName().toString();
        if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
          Files.deleteIfExists(file);
        }
      }
    }
  }

  private static Path copyRulesetZip(RulesetSpec ruleset, File rulesetZip, Path rulesetDirectory)
      throws IOException {
    final var target = rulesetDirectory.resolve(ruleset.folderName() + ".zip");
    Files.copy(rulesetZip.toPath(), target, REPLACE_EXISTING);
    return target;
  }

  private static Path copyDependencyJar(Artifact artifact, Path rulesetDirectory)
      throws IOException {
    var target = rulesetDirectory.resolve(artifact.getArtifactId() + ".jar");
    if (Files.exists(target)) {
      target =
          rulesetDirectory.resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar");
    }
    Files.copy(artifact.getFile().toPath(), target, REPLACE_EXISTING);
    return target;
  }

  private List<String> resolveDependencyTrailRoots(RulesetSpec ruleset)
      throws MojoExecutionException {
    final var dependencyTrailRoots = new LinkedHashSet<String>();
    collectDependencyTrailRoots(
        buildPomProject(ruleset.toPomDependency()), dependencyTrailRoots, new HashSet<>());
    return List.copyOf(dependencyTrailRoots);
  }

  private RulesetSpec toRulesetSpec(
      Dependency rulesetZipDependency, Map<String, String> rulesetFolderNames) {
    final var coordinates =
        rulesetZipDependency.getGroupId() + ":" + rulesetZipDependency.getArtifactId();
    final var folderName = rulesetFolderNames.get(coordinates);
    if (folderName == null) {
      throw new IllegalStateException(
          "No ruleset folder mapping configured for declared dependency " + coordinates);
    }
    return new RulesetSpec(
        folderName,
        rulesetZipDependency.getGroupId(),
        rulesetZipDependency.getArtifactId(),
        Objects.requireNonNull(rulesetZipDependency.getVersion()));
  }

  private Map<String, String> resolveRulesetFolderNames() throws MojoExecutionException {
    if (rulesetFolderMappings == null || rulesetFolderMappings.isEmpty()) {
      getLog()
          .error(
              "Missing required rulesetFolderMappings configuration. "
                  + "Configure entries in the format '<groupId>:<artifactId>=<folderName>'.");
      throw new MojoExecutionException("Missing required rulesetFolderMappings configuration");
    }

    final var mappings = new LinkedHashMap<String, String>();
    for (final var mapping : rulesetFolderMappings) {
      final var separatorIndex = mapping.indexOf('=');
      if (separatorIndex <= 0 || separatorIndex == mapping.length() - 1) {
        throw new IllegalStateException(
            "Invalid rulesetFolderMappings entry '"
                + mapping
                + "'. Expected format '<groupId>:<artifactId>=<folderName>'.");
      }

      mappings.put(mapping.substring(0, separatorIndex), mapping.substring(separatorIndex + 1));
    }
    return Map.copyOf(mappings);
  }

  private static boolean isRulesetPomDependency(Dependency dependency) {
    return "pom".equals(normalizedType(dependency)) && normalizedClassifier(dependency).isEmpty();
  }

  private File resolveRulesetZip(RulesetSpec ruleset) throws MojoExecutionException {
    try {
      return Objects.requireNonNull(
          requireRepositorySystem()
              .resolveArtifact(
                  requireRepositorySystemSession(),
                  new ArtifactRequest()
                      .setRepositories(requireRemoteProjectRepositories())
                      .setArtifact(ruleset.toRulesetZipArtifact()))
              .getArtifact()
              .getFile(),
          "Resolved ruleset ZIP artifact has no file");
    } catch (ArtifactResolutionException e) {
      throw new MojoExecutionException(
          "Failed to resolve ruleset ZIP for " + ruleset.coordinates() + ":" + ruleset.version(),
          e);
    }
  }

  private Dependency withResolvedVersion(Dependency dependency) {
    if (dependency.getVersion() != null && !dependency.getVersion().isBlank()) {
      return dependency;
    }

    final var dependencyManagement = requireProject().getDependencyManagement();
    final var managedDependency =
        Objects.requireNonNull(
                dependencyManagement, "Current project has no dependencyManagement section")
            .getDependencies()
            .stream()
            .filter(candidate -> sameDependencyCoordinates(candidate, dependency))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Could not resolve managed version for dependency "
                            + dependency.getGroupId()
                            + ":"
                            + dependency.getArtifactId()));

    final var resolvedDependency = dependency.clone();
    resolvedDependency.setVersion(managedDependency.getVersion());
    return resolvedDependency;
  }

  private static boolean sameDependencyCoordinates(Dependency left, Dependency right) {
    return left.getGroupId().equals(right.getGroupId())
        && left.getArtifactId().equals(right.getArtifactId())
        && normalizedType(left).equals(normalizedType(right))
        && normalizedClassifier(left).equals(normalizedClassifier(right));
  }

  private MavenProject buildPomProject(Dependency dependency) throws MojoExecutionException {
    try {
      final var pomArtifactFile =
          requireRepositorySystem()
              .resolveArtifact(
                  requireRepositorySystemSession(),
                  new ArtifactRequest()
                      .setRepositories(requireRemoteProjectRepositories())
                      .setArtifact(
                          new DefaultArtifact(
                              dependency.getGroupId(),
                              dependency.getArtifactId(),
                              "pom",
                              dependency.getVersion())))
              .getArtifact()
              .getFile();

      final var projectBuildingRequest =
          new DefaultProjectBuildingRequest(requireSession().getProjectBuildingRequest());
      projectBuildingRequest.setResolveDependencies(false);

      return requireProjectBuilder()
          .build(
              Objects.requireNonNull(pomArtifactFile, "Resolved pom artifact has no file"),
              projectBuildingRequest)
          .getProject();
    } catch (ArtifactResolutionException | ProjectBuildingException e) {
      throw new MojoExecutionException(
          "Failed to read upstream pom for "
              + dependency.getGroupId()
              + ":"
              + dependency.getArtifactId()
              + ":"
              + dependency.getVersion(),
          e);
    }
  }

  private void collectDependencyTrailRoots(
      MavenProject pomProject,
      LinkedHashSet<String> dependencyTrailRoots,
      HashSet<String> visitedPoms)
      throws MojoExecutionException {
    final var pomCoordinates = pomProject.getGroupId() + ":" + pomProject.getArtifactId();
    if (!visitedPoms.add(pomCoordinates)) {
      return;
    }

    for (final var dependency : pomProject.getDependencies()) {
      if (shouldSkipDependency(dependency)) {
        continue;
      }

      if ("pom".equals(normalizedType(dependency))) {
        collectDependencyTrailRoots(buildPomProject(dependency), dependencyTrailRoots, visitedPoms);
        continue;
      }

      dependencyTrailRoots.add(dependency.getGroupId() + ":" + dependency.getArtifactId());
    }
  }

  private static boolean shouldSkipDependency(Dependency dependency) {
    return "test".equals(dependency.getScope())
        || "provided".equals(dependency.getScope())
        || "system".equals(dependency.getScope())
        || "import".equals(dependency.getScope())
        || Boolean.parseBoolean(dependency.getOptional());
  }

  private static String normalizedType(Dependency dependency) {
    return dependency.getType() == null || dependency.getType().isBlank()
        ? "jar"
        : dependency.getType();
  }

  private static String normalizedClassifier(Dependency dependency) {
    return dependency.getClassifier() == null ? "" : dependency.getClassifier();
  }

  private RepositorySystem requireRepositorySystem() {
    return Objects.requireNonNull(repositorySystem, "Maven RepositorySystem was not injected");
  }

  private ProjectBuilder requireProjectBuilder() {
    return Objects.requireNonNull(projectBuilder, "Maven ProjectBuilder was not injected");
  }

  private MavenProject requireProject() {
    return Objects.requireNonNull(project, "Current Maven project was not injected");
  }

  private MavenSession requireSession() {
    return Objects.requireNonNull(session, "Current Maven session was not injected");
  }

  private RepositorySystemSession requireRepositorySystemSession() {
    return Objects.requireNonNull(
        repositorySystemSession, "Maven RepositorySystemSession was not injected");
  }

  private List<RemoteRepository> requireRemoteProjectRepositories() {
    return Objects.requireNonNull(
        remoteProjectRepositories, "Current project remote repositories were not injected");
  }

  private File requireOutputDirectory() {
    return Objects.requireNonNull(outputDirectory, "Output directory was not injected");
  }

  private record RulesetSpec(String folderName, String groupId, String artifactId, String version) {

    String coordinates() {
      return groupId + ":" + artifactId;
    }

    Dependency toPomDependency() {
      final var pomDependency = new Dependency();
      pomDependency.setGroupId(groupId);
      pomDependency.setArtifactId(artifactId);
      pomDependency.setVersion(version);
      pomDependency.setType("pom");
      return pomDependency;
    }

    DefaultArtifact toRulesetZipArtifact() {
      return new DefaultArtifact(groupId, artifactId, "ruleset", "zip", version);
    }

    boolean isTransitiveLibraryJar(Artifact artifact, List<String> dependencyTrailRoots) {
      if (!"jar".equals(artifact.getType())) {
        return false;
      }
      if (artifact.getClassifier() != null && !artifact.getClassifier().isBlank()) {
        return false;
      }
      if (artifact.getFile() == null) {
        return false;
      }
      if ("test".equals(artifact.getScope()) || "provided".equals(artifact.getScope())) {
        return false;
      }

      final var dependencyTrail = artifact.getDependencyTrail();
      if (dependencyTrail == null || dependencyTrail.isEmpty()) {
        return false;
      }

      return dependencyTrail.stream()
          .anyMatch(
              trailEntry ->
                  dependencyTrailRoots.stream()
                      .anyMatch(root -> trailEntry.startsWith(root + ":")));
    }
  }
}
