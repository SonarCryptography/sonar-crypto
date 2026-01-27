package org.sonarcrypto;
import crysl.parsing.CrySLModelReader;
import crysl.parsing.CrySLModelReaderClassPath;
import crysl.rule.CrySLRule;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.utils.ResourceEnumerator;

import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws Exception {
		
		final var classPath = Path.of(args[0]);
		//final var rulesStream = Main.class.getClassLoader().getResourceAsStream("crysl_rules");
		
		Path tempDir = Files.createTempDirectory("crysl_rules");
		
		System.out.println(tempDir);
		
		var cryslResourcePaths = ResourceEnumerator.listResources(Path.of("crysl_rules"), "-ruleset.zip");
		System.out.println(cryslResourcePaths);
		
		for(final var cryslResourcePath : cryslResourcePaths) {
			String string = cryslResourcePath.toString();
			System.out.println(string);
			try(var cryslResource = Main.class.getResourceAsStream(cryslResourcePath.toString())) {

				if(cryslResource == null) {
					System.out.println("Resource is null!");
					throw new RuntimeException("Failed extracting CrySL rules: The resource stream is null!");
				}

				Files.copy(cryslResource, tempDir.resolve(cryslResourcePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		
		final var scanner =
			new HeadlessJavaScanner(classPath.toString(), tempDir.resolve("JavaCryptographicArchitecture-3.1.4-ruleset.zip").toString());

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
