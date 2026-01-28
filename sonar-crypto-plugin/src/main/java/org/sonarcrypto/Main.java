package org.sonarcrypto;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.rules.CryslRuleProvider;

import java.nio.file.Path;

public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws Exception {
		final var classPath = Path.of(args[0]);
		final var ruleSetFile =
			new CryslRuleProvider().extractCryslFileToTempDir(args[1]).toString();
		
		final var scanner =
			new HeadlessJavaScanner(
				classPath.toString(),
				ruleSetFile
			);

		scanner.setFramework(ScannerSettings.Framework.SOOT);
		scanner.scan();
		var errors = scanner.getCollectedErrors();
		System.out.println(errors);
	}
	
	//public static void main(String[] args) throws Exception {
	//	CryslRuleProvider ruleProvider = new CryslRuleProvider();
	//	Path ruleDir =
	//		ruleProvider.extractCryslFilesToTempDir(s -> s.contains("JavaCryptographicArchitecture/"));
	//	String mavenProjectPath = new File(args[0]).getAbsolutePath();
	//	try {
	//		MavenProject mi = new MavenProject(mavenProjectPath);
	//		mi.compile();
	//		LOGGER.info("Built project to directory: {}", mi.getBuildDirectory());
	//		HeadlessJavaScanner scanner =
	//			new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());
	//		
	//		scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
	//		scanner.scan();
	//		var errors = scanner.getCollectedErrors();
	//		LOGGER.info("Errors: {}", errors.size());
	//	} catch (MavenBuildException e) {
	//		LOGGER.error("Failed to build project", e);
	//	}
	//}
}
