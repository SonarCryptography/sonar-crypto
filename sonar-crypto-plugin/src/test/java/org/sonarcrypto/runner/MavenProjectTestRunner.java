package org.sonarcrypto.runner;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.Ruleset;
import org.sonarcrypto.cognicrypt.MavenBuildException;
import org.sonarcrypto.cognicrypt.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

@NullMarked
public non-sealed class MavenProjectTestRunner extends TestRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(MavenProjectTestRunner.class);
	
	public MavenProjectTestRunner() {
	}
	
	public MavenProjectTestRunner(final Framework framework) {
		super(framework);
	}
	
	
	/**
	 * Runs the analysis.
	 *
	 * @param path The maven project path.
	 * @param ruleset The ruleset.
	 * @return The analysis result.
	 * @throws IOException An I/O error is occurred.
	 */
	@Override
	public Table<WrappedClass, Method, Set<AbstractError>> run(
		final String path,
		final Ruleset ruleset
	) throws IOException, URISyntaxException {
		final var mavenProjectPath = new File(path).getAbsolutePath();
		final String classPath;
		
		try {
			MavenProject mavenProject = new MavenProject(mavenProjectPath);
			mavenProject.compile();
			classPath = mavenProject.getBuildDirectory();
			LOGGER.info("Built project to directory: {}", classPath);
		}
		catch(MavenBuildException e) {
			LOGGER.error("Failed to build project", e);
			System.exit(1);
			throw new Error();
		}
		
		LOGGER.info("Maven project: {}", classPath);
		
		return super.run(classPath, ruleset);
	}
}
