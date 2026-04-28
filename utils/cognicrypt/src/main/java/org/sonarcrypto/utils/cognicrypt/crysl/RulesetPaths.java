package org.sonarcrypto.utils.cognicrypt.crysl;

import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;

/**
 * Holds the paths extracted for a CrySL ruleset: the ruleset ZIP file and the classpath string of
 * the library JARs the rules describe.
 */
@NullMarked
public record RulesetPaths(Path rulesetZip, String dependencyClasspath) {}
