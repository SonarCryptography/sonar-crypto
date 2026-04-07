package org.sonarcrypto.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.orchestrator.build.BuildResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class E2ETests extends OrchestratorTests {
  private static final String JAVA_MAVEN_BASIC_PATH = "src/test/resources/Java/Maven/Basic";

  @Test
  void java_maven_basic() {
    BuildResult result = executeMavenBuild(new File(JAVA_MAVEN_BASIC_PATH), "java-maven-basic");

    String log = result.getLogs();
    List<Integer> logIndices = new ArrayList<>();
    if (areSonarPrivatePluginsAvailable()) {
      logIndices.add(log.indexOf("Sensor JavaModuleSecuritySensor [securityjavafrontend]"));
      logIndices.add(log.indexOf("Sensor UCFG Bridge [ucfgbridge]"));
      logIndices.add(log.indexOf("UCFG Bridge [jimple]: 26 UCFGs read from"));
      logIndices.add(log.indexOf("Sensor CogniCryptSensor [crypto]"));
      logIndices.add(log.indexOf("Using Jimple files from bridge output"));
    } else {
      logIndices.add(log.indexOf("Sensor CogniCryptSensor [crypto]"));
      logIndices.add(log.indexOf("No Jimple files found at"));
    }
    logIndices.add(log.indexOf("Found 3 cryptographic errors"));

    for (int i = 0; i < logIndices.size() - 1; i++) {
      assertThat(logIndices.get(i)).isLessThan(logIndices.get(i + 1));
    }
  }
}
