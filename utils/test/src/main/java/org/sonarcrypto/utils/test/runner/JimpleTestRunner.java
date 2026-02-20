package org.sonarcrypto.utils.test.runner;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.cognicrypt.JimpleScanner;
import org.sonarcrypto.utils.cognicrypt.crysl.CryslRuleProvider;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;

@NullMarked
public non-sealed class JimpleTestRunner
    extends TestRunner<Table<WrappedClass, Method, Set<AbstractError>>> {
  /**
   * Runs the analysis.
   *
   * @param path The class path.
   * @param ruleset The ruleset.
   * @return The analysis result.
   * @throws IOException An I/O error is occurred.
   */
  @Override
  public Table<WrappedClass, Method, Set<AbstractError>> run(
      final String path, final Ruleset ruleset) throws IOException, URISyntaxException {

    final var provider = new CryslRuleProvider();
    final var scanner =
        new JimpleScanner(path, provider.extractRulesetToTempDir(ruleset).toString());
    scanner.scan();
    return scanner.getCollectedErrors();
  }
}
