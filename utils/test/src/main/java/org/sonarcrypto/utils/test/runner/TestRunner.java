package org.sonarcrypto.utils.test.runner;

import java.io.IOException;
import java.net.URISyntaxException;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;

@NullMarked
public abstract sealed class TestRunner<RESULT>
    permits ClassPathTestRunner, MavenProjectTestRunner, JimpleTestRunner {

  public abstract RESULT run(final String path, final Ruleset ruleset)
      throws IOException, URISyntaxException;
}
