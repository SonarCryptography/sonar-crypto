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
				System.err.println("Cannot set both args --classPath and --mvnProject");
				LOGGER.error("Cannot set both args --classPath and --mvnProject");
				System.exit(1);
			}
			
			classPath = classPathArg;
			
			System.out.println("Class path: " + classPath);
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
				throw new RuntimeException();
			}
			
			System.out.println("Maven project: " + classPath);
		}
		else {
			System.err.println(
				"Invalid command line arguments: class path or maven project required."
			);
			LOGGER.error("Invalid command line arguments: class path or maven project required.");
			System.exit(1);
			throw new RuntimeException();
		}
		
		final var rulesetFile =
			new CryslRuleProvider().extractCryslFileToTempDir(ruleset);
		
		if(rulesetFile == null) {
			System.err.println("CrySL ruleset not found!");
			LOGGER.error("CrySL ruleset not found!");
			System.exit(1);
		}
		
		System.out.println("Ruleset: " + ruleset);
		System.out.println("Framework: " + framework);
		System.out.println();
		
		final var scanner = new HeadlessJavaScanner(classPath, rulesetFile.toString());
		scanner.setFramework(framework);
		
		System.out.println("Running analysis ...");
		scanner.scan();
		System.out.println("Done.");
		System.out.println();
		
		var errors = scanner.getCollectedErrors();
		
		if(errors.isEmpty()) {
			System.out.println("No vulnerabilities found.");
			return;
		}
		
		System.out.println("Found " + errors.size() + " vulnerabilities:");
		System.out.println();
		System.out.println(errors);
	}
}
