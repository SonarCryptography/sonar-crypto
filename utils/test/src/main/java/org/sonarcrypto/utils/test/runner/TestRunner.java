package org.sonarcrypto.utils.test.runner;

import java.io.IOException;
import java.net.URISyntaxException;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;

@NullMarked
public abstract sealed class TestRunner<Result>
    permits ClassPathTestRunner, MavenProjectTestRunner, JimpleTestRunner {

  public abstract Result run(final String path, final Ruleset ruleset)
      throws IOException, URISyntaxException;
}
