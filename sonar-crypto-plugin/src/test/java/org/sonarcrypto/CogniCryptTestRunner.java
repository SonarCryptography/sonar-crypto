package org.sonarcrypto;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.rules.CryslRuleProvider;

import java.io.IOException;
import java.util.Set;

@NullMarked
public class CogniCryptTestRunner {
	
	private final Framework framework;
    
    /**
     * Creates a new instance with the SootUp framework.
     */
	public CogniCryptTestRunner() {
		this.framework = Framework.SOOT_UP;
	}
    
    /**
     * Creates a new instance with the give framework.
     */
	public CogniCryptTestRunner(Framework framework) {
		this.framework = framework;
	}
	
	public Table<WrappedClass, Method, Set<AbstractError>> run(
		String applicationPath,
		Ruleset ruleset
	) throws IOException {
		var provider = new CryslRuleProvider();
		var scanner = new HeadlessJavaScanner(
			applicationPath,
			provider.extractRulesetToTempDir(ruleset.getRulesetName()).toString()
		);
		scanner.setFramework(framework);
		scanner.scan();
		return scanner.getCollectedErrors();
	}
}
