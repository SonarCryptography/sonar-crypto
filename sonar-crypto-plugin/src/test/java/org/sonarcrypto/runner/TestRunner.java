package org.sonarcrypto.runner;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.Ruleset;
import org.sonarcrypto.rules.CryslRuleProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

@NullMarked
public sealed abstract class TestRunner permits ClassPathTestRunner, MavenProjectTestRunner {
	
	private final Framework framework;
    
    /**
     * Creates a new instance with the SootUp framework.
     */
	public TestRunner() {
		this.framework = Framework.SOOT_UP;
	}
    
    /**
     * Creates a new instance with the give framework.
     */
	public TestRunner(final Framework framework) {
		this.framework = framework;
	}
	
	public Table<WrappedClass, Method, Set<AbstractError>> run(
		final String path,
		final Ruleset ruleset
	) throws IOException, URISyntaxException {
		final var provider = new CryslRuleProvider();
		final var scanner = new HeadlessJavaScanner(
			path,
			provider.extractRulesetToTempDir(ruleset.getRulesetName()).toString()
		);
		scanner.setFramework(framework);
		scanner.scan();
		return scanner.getCollectedErrors();
	}
}
