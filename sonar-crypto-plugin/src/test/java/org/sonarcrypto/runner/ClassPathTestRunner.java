package org.sonarcrypto.runner;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.Ruleset;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

@NullMarked
public non-sealed class ClassPathTestRunner extends TestRunner {
	
	public ClassPathTestRunner() {
	}
	
	public ClassPathTestRunner(final Framework framework) {
		super(framework);
	}
	
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
		final String path,
		final Ruleset ruleset
	) throws IOException, URISyntaxException {
		return super.run(path, ruleset);
	}
}
