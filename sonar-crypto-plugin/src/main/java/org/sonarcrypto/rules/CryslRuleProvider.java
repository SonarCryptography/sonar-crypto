package org.sonarcrypto.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.utils.ResourceExtractor;

/**
 * Provides a method to download and extract CrySL rules from the CROSSING repository. The repo
 * currently contains rules for three Crypto Libraries : BouncyCastle, BouncyCastle-JCA, and
 * JavaCryptographicArchitecture. The rulesets can not be used at the same time due to conflicting
 * file names. The rules are extracted to a temporary directory.
 */
@NullMarked
public class CryslRuleProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CryslRuleProvider.class);
	
	public @Nullable Path extractCryslFileToTempDir(String ruleset) throws IOException {
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
			return null;
		if(foundRules > 1)
			LOGGER.error("Multiple rule sets matched to {}; using first rule set.", ruleset);
		
		return extractedRulePaths.get(0);
	}
}
