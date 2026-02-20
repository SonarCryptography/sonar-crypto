package org.sonarcrypto.e2e;

import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;
import org.sonarcrypto.utils.test.asserts.CcErrorsAssert;
import org.sonarcrypto.utils.test.runner.JimpleTestRunner;
import org.sonarcrypto.utils.test.runner.MavenProjectTestRunner;

public class JavaAndJimpleAnalysisResultsTest {
  @Test
  public void testEnsureEqualJavaAndJimpleResults() throws IOException, URISyntaxException {
    for (final var ruleset : Ruleset.values()) {
      final var javaResult =
          new MavenProjectTestRunner().run("src/test/resources/Java/Maven/Basic", ruleset);
      final var mavenProject = javaResult.mavenProject();
      final var javaAnalysisResult = javaResult.collectedErrors();

      final var jimpleAnalysisResult =
          new JimpleTestRunner().run(mavenProject.getJimpleDirectory(), ruleset);

      CcErrorsAssert.assertEquals(
          "Different results with ruleset " + ruleset + "!",
          javaAnalysisResult,
          jimpleAnalysisResult);
    }
  }
}
