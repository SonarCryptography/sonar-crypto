package org.sonarcrypto.utils.test.runner;

import static de.fraunhofer.iem.scanner.ScannerSettings.Framework.SOOT_UP;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.cognicrypt.crysl.CryslRuleProvider;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;

@NullMarked
public abstract sealed class TestRunner permits ClassPathTestRunner, MavenProjectTestRunner {

  public Table<WrappedClass, Method, Set<AbstractError>> run(
      final String path, final Ruleset ruleset) throws IOException, URISyntaxException {
    final var provider = new CryslRuleProvider();
    final var scanner =
        new HeadlessJavaScanner(
            path, provider.extractRulesetToTempDir(ruleset.getRulesetName()).toString());
    scanner.setFramework(SOOT_UP);
    scanner.scan();
    return scanner.getCollectedErrors();
  }
}
