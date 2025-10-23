package org.sonarcrypto.e2e;

import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.junit5.OrchestratorExtension;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.RegisterExtension;

class OrchestratorTests {
  private static final String SONAR_QUBE_VERSION = "25.10.0.114319";

  @RegisterExtension
  private static final OrchestratorExtension ORCHESTRATOR = OrchestratorExtension.builderEnv()
    .useDefaultAdminCredentialsForBuilds(true)
    .setSonarVersion(SONAR_QUBE_VERSION)
    // TODO: Activate sonar-crypto-plugin once working
    // .addPlugin(FileLocation.of(sonarCryptoJar(BUILD_DIR_PATH)))
    // TODO: Remove sonar-java-plugin once sonar-crypto-plugin comes with a default quality profile
    .addPlugin(MavenLocation.of("org.sonarsource.java", "sonar-java-plugin", "8.20.0.40630"))
    .build();

  @AfterAll
  static void endTest() {
    // Put breakpoint here for debugging purposes
    ORCHESTRATOR.stop();
  }

  BuildResult executeMavenBuild(File projectLocation, String projectKey) {
    return executeMavenBuild(projectLocation, projectKey, null);
  }

  BuildResult executeMavenBuild(File projectLocation, String projectKey, @Nullable Map<String, String> properties) {
    MavenBuild build = MavenBuild
      .create(new File(projectLocation, "pom.xml"))
      // TODO: Add -Dscan=false after -DskipTests?
      .setGoals("clean package -DskipTests -Dsonar.projectKey=" + projectKey + " sonar:sonar");

    // Propagate MAVEN_OPTS to Maven Scanner for debugging purposes
    String mavenOpts = System.getenv("MAVEN_OPTS");
    if (mavenOpts != null) {
      build.setEnvironmentVariable("MAVEN_OPTS", mavenOpts);
    }

    // Set properties
    build.setProperty("sonar.cpd.exclusions", "**/*")
      .setProperty("sonar.internal.analysis.failFast", "true")
      .setProperty("sonar.scanner.skipJreProvisioning", "true");
    if (properties != null) {
      build.setProperties(properties);
    }

    return ORCHESTRATOR.executeBuild(build);
  }
}
