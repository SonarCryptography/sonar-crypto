package org.sonarcrypto;

import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.cognicrypt.MavenBuildException;
import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;

import java.io.File;

@NullMarked
public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
	public static void main(final String[] args) throws Exception {
		final var cliArgs = CliArgs.parse(args);
		
		final var classPathArg = cliArgs.getClassPath();
		final var mvnProject = cliArgs.getMvnProject();
		final var ruleset = cliArgs.getRuleset();
		final var framework = cliArgs.getFramework();
		
		final String classPath;
		
		if(classPathArg != null) {
			if(mvnProject != null) {
				LOGGER.error("Cannot set both class path and maven project at the same time");
				System.exit(1);
				throw new Error();
			}
			
			classPath = classPathArg;
			
			LOGGER.info("Class path: {}", classPath);
		}
		else if(mvnProject != null) {
			String mavenProjectPath = new File(mvnProject).getAbsolutePath();
			
			try {
				MavenProject mi = new MavenProject(mavenProjectPath);
				mi.compile();
				classPath = mi.getBuildDirectory();
				LOGGER.info("Built project to directory: {}", classPath);
			}
			catch(MavenBuildException e) {
				LOGGER.error("Failed to build project", e);
				System.exit(1);
				throw new Error();
			}
			
			LOGGER.info("Maven project: {}", classPath);
		}
		else {
			LOGGER.error("Invalid command line arguments: class path or maven project required.");
			System.exit(1);
			throw new Error();
		}
		
		final var rulesetFile =
			new CryslRuleProvider().extractCryslFileToTempDir(ruleset);
		
		if(rulesetFile == null) {
			LOGGER.error("CrySL ruleset not found!");
			System.exit(1);
		}
		
		LOGGER.info("Ruleset: {}", ruleset);
		LOGGER.info("Framework: {}", framework);
		
		final var scanner = new HeadlessJavaScanner(classPath, rulesetFile.toString());
		scanner.setFramework(framework);
		
		LOGGER.info("Running analysis ...");
		scanner.scan();
		LOGGER.info("Done.");
		
		var errors = scanner.getCollectedErrors();
		
		if(errors.isEmpty()) {
			LOGGER.info("No vulnerabilities found.");
			return;
		}
		
		LOGGER.info("Found {} vulnerabilities: {}", errors.size(), errors);
	}
}
