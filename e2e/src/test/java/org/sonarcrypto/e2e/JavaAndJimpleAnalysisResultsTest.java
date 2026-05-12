package org.sonarcrypto.e2e;

import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;
import org.sonarcrypto.utils.test.asserts.CcErrorsAssert;
import org.sonarcrypto.utils.test.runner.JimpleTestRunner;
import org.sonarcrypto.utils.test.runner.MavenProjectTestRunner;

public class JavaAndJimpleAnalysisResultsTest {
  @Test
  void java_and_jimple_results_match_for_jca() throws IOException, URISyntaxException {
    assertResultsEqual(Ruleset.JCA);
  }

  @Test
  void java_and_jimple_results_match_for_bc() throws IOException, URISyntaxException {
    assertResultsEqual(Ruleset.BC);
  }

  @Ignore(
      "Tink cognicrypt analyses fail since boomerang calls Class.forName This test should be re-enabled once the issue is resolved.")
  void java_and_jimple_results_match_for_tink() throws IOException, URISyntaxException {
    assertResultsEqual(Ruleset.TINK);
  }

  @Test
  void java_and_jimple_results_match_for_bc_jca() throws IOException, URISyntaxException {
    assertResultsEqual(Ruleset.BC_JCA);
  }

  private void assertResultsEqual(Ruleset ruleset) throws IOException, URISyntaxException {
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
