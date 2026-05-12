package org.sonarcrypto.utils.cognicrypt.crysl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.utils.resource.ResourceExtractor;

/** Provides a method to extract a CrySL ruleset from the resources into a temporary directory. */
@NullMarked
public class CryslRuleProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(CryslRuleProvider.class);

  /**
   * Extracts a CrySL ruleset ZIP file and its library JARs into a temporary directory.
   *
   * @param ruleset The ruleset.
   * @return The extracted ruleset paths.
   * @throws IOException An I/O error occurred.
   * @throws URISyntaxException Should never occur, because the URI should always be well-defined.
   */
  public RulesetPaths extractRulesetToTempDir(Ruleset ruleset)
      throws IOException, URISyntaxException {
    return extractRulesetToTempDir(ruleset.getRulesetName());
  }

  RulesetPaths extractRulesetToTempDir(String ruleset) throws IOException, URISyntaxException {
    final var rulesFolderName = "crysl_rules";
    final var rulesetFolderName = rulesFolderName + "/" + ruleset;
    final var tempDir = Files.createTempDirectory(rulesFolderName);
    final var tempRulesetDir = Files.createDirectory(tempDir.resolve(ruleset));

    final var extractedRulePaths =
        ResourceExtractor.extract(
            rulesetFolderName, tempRulesetDir, ".zip", ruleset::equalsIgnoreCase);
    if (extractedRulePaths.isEmpty()) throw new IOException("CrySL ruleset name not found");
    if (extractedRulePaths.size() > 1)
      LOGGER.error("Multiple rule sets matched to {}; using first rule set.", ruleset);

    final var extractedDependencyPaths =
        ResourceExtractor.extract(rulesetFolderName, tempRulesetDir, ".jar", ignored -> true);

    final var dependencyClasspath =
        new HashSet<>(extractedDependencyPaths)
            .stream().map(Path::toString).collect(Collectors.joining(":"));

    return new RulesetPaths(extractedRulePaths.get(0), dependencyClasspath);
  }
}
