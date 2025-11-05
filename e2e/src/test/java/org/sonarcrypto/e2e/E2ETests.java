package org.sonarcrypto.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.orchestrator.build.BuildResult;
import java.io.File;
import org.junit.jupiter.api.Test;

class E2ETests extends OrchestratorTests {
  private static final String JAVA_MAVEN_BASIC_PATH = "src/test/resources/Java/Maven/Basic";

  @Test
  void java_maven_basic() {
    BuildResult result = executeMavenBuild(new File(JAVA_MAVEN_BASIC_PATH), "java-maven-basic");
    // TODO: Replace with sonar-crypto-plugin sensor log check once available
    assertThat(result.getLogsLines(s -> s.contains("Sensor JavaProjectSensor [java] (done)")))
        .hasSize(1);
  }
}
