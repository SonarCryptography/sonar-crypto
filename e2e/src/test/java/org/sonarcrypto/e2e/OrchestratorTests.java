package org.sonarcrypto.e2e;

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonarcrypto.e2e.utility.FileUtilities;

class OrchestratorTests {
  private static final String SONAR_MAVEN_PLUGIN_VERSION = "5.5.0.6356";

  @RegisterExtension
  private static final OrchestratorExtension ORCHESTRATOR =
      OrchestratorExtension.builderEnv()
          .setZipFile(FileUtilities.findFile("target", "sq_for_orchestrator-", ".zip"))
          .useDefaultAdminCredentialsForBuilds(true)
          .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "8.22.0.41895"))
          .addPlugin(
              FileLocation.of(
                  FileUtilities.findFile(
                      "../sonar-crypto-plugin/target", "sonar-crypto-plugin", ".jar")))
          .build();

  @TempDir File tempDir;

  @AfterAll
  static void endTest() {
    // Put breakpoint here for debugging purposes
    ORCHESTRATOR.stop();
  }

  BuildResult executeMavenBuild(File projectLocation, String projectKey) {
    return executeMavenBuild(projectLocation, projectKey, null);
  }

  BuildResult executeMavenBuild(
      File projectLocation, String projectKey, @Nullable Map<String, String> properties) {
    MavenBuild build =
        MavenBuild.create(new File(projectLocation, "pom.xml"))
            .setGoals(
                "clean package -DskipTests -Dsonar.projectKey="
                    + projectKey
                    + " org.sonarsource.scanner.maven:sonar-maven-plugin:"
                    + SONAR_MAVEN_PLUGIN_VERSION
                    + ":sonar");

    // Propagate MAVEN_OPTS to Maven Scanner for debugging purposes
    String mavenOpts = System.getenv("MAVEN_OPTS");
    if (mavenOpts != null) {
      build.setEnvironmentVariable("MAVEN_OPTS", mavenOpts);
    }

    // Set properties
    build
        .setProperty("sonar.cpd.exclusions", "**/*")
        .setProperty("sonar.internal.analysis.failFast", "true")
        .setProperty("sonar.scanner.skipJreProvisioning", "true")
        .setProperty("sonar.working.directory", tempDir.getAbsolutePath());
    if (properties != null) {
      build.setProperties(properties);
    }

    return ORCHESTRATOR.executeBuild(build);
  }
}
