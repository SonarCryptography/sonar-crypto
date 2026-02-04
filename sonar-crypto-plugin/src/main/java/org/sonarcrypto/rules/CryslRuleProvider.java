package org.sonarcrypto.rules;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.utils.ResourceExtractor;

/**
 * Provides a method to extract a CrySL ruleset from the resources into a temporary directory.
 */
@NullMarked
public class CryslRuleProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CryslRuleProvider.class);
	
	/**
	 * Extracts a CrySL ruleset ZIP file into a temporary directory.
	 * 
	 * @param ruleset The ruleset name, i.e., "bc", "bc-jca", "jca", or "tink".
	 * @return The path to the extracted ruleset ZIP file;
	 *         or {@code null}, if the given ruleset name was not found.
	 * @throws IOException An I/O error occurred.
	 * @throws URISyntaxException Should never occur, because the URI should always be well-defined.
	 */
	public Path extractRulesetToTempDir(String ruleset) throws IOException, URISyntaxException {
		final var fileEnding = ".zip";
		final var rulesFolderName = "crysl_rules";
		final var tempDir = Files.createTempDirectory(rulesFolderName);
		final var extractedRulePaths = ResourceExtractor.extract(
			rulesFolderName,
			tempDir,
			fileEnding,
			ruleset::equalsIgnoreCase
		);
		
		final var foundRules = extractedRulePaths.size();
		
		if(foundRules == 0)
			throw new IOException("CrySL ruleset name not found");
		if(foundRules > 1)
			LOGGER.error("Multiple rule sets matched to {}; using first rule set.", ruleset);
		
		return extractedRulePaths.get(0);
	}
}
