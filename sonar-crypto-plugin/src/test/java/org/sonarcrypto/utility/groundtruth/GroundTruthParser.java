package org.sonarcrypto.utility.groundtruth;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.causes.CallCause;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.ccerror.causes.ValueCause;

/**
 * Parses the ground truth specification of a Java project.
 *
 * <p><b>Syntax:</b>
 *
 * <pre><code>
 * // CC: RULE_KIND1/CausedBy2 "a value", RULE_KIND2/CausedBy3, RULE_KIND2/CausedBy4 "another value"
 * </code></pre>
 *
 * <p>Matches for {@code "CC:"} up to the line end or to a semicolon, allowing to prefix or suffix
 * the ground truth specification with custom text. The entries are separated by a comma.
 *
 * <pre><code>
 * // Any text; CC: RULE_KIND/CausedBy "a value"; any text
 * </code></pre>
 *
 * <p>Optional entries that may be reported but do not need to be, can be specified as optional via
 * the {@code "[?]"} marker:
 *
 * <pre><code>
 * // CC: [?] RULE_KIND/CausedBy "a value"
 * </code></pre>
 *
 * <p>Note: Specifications in commented code (e.g., {@code // cipher.init(...) // CC: ...}) are
 * ignored.
 *
 * <p><b>Valid values:</b>
 *
 * <p><i>Rule kinds:</i> All values of the {@link RuleKind} enumeration.
 *
 * <p><i>Causes:</i> The names of all non-abstract {@link Cause cause classes} without the {@code
 * "Cause"} suffix.
 *
 * <p>The following causes require a value specification:
 *
 * <table>
 *     <tr>
 *         <th>Cause</th>
 *         <th>Value</th>
 *     </tr>
 *     <tr>
 *         <td>ForbiddenMethod</td>
 *         <td>Short name of the method, e.g. "Foo.bar"</td>
 *     </tr>
 *     <tr>
 *         <td>ForbiddenType</td>
 *         <td>Full-qualified class name, e.g. "org.example.Foo"</td>
 *     </tr>
 *     <tr>
 *         <td>IncompleteOperation</td>
 *         <td>Full-qualified class name, e.g. "org.example.Foo"</td>
 *     </tr>
 *     <tr>
 *         <td>InvalidValue</td>
 *         <td>The invalid value in quotes, no matter if it is a string or an integer value, e.g. "ECB"</td>
 *     </tr>
 *     <tr>
 *         <td>UncaughtException</td>
 *         <td>Full-qualified class name, e.g. "org.example.Foo"</td>
 *     </tr>
 *     <tr>
 *         <td>UnexpectedCall</td>
 *         <td>Short name of the method, e.g. "Foo.bar"</td>
 *     </tr>
 * </table>
 *
 * <p><b>Example:</b>
 *
 * <pre><code>
 * public byte[] encryptWithECB(byte[] data) throws Exception {
 *     Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding"); // CC: MODE/InvalidValue "ECB"
 *
 *     SecretKeySpec keySpec = new SecretKeySpec(HARDCODED_KEY.getBytes(), "AES"); // CC: KEY_MATERIAL/ForbiddenType "java.lang.String", KEY_MATERIAL/ImproperGenerated
 *
 *     cipher.init(Cipher.ENCRYPT_MODE, keySpec); // Inits the Cipher object; CC: [?] KEY_MATERIAL/ImproperGenerated
 * }
 * </code></pre>
 */
@NullMarked
public class GroundTruthParser {
  private static final Map<String, Class<? extends Cause>> CAUSES;

  static {
    final var callCauses = Stream.of(CallCause.class.getPermittedSubclasses());
    final var valueCauses = Stream.of(ValueCause.class.getPermittedSubclasses());
    final var allCauses = Stream.concat(callCauses, valueCauses);
    final var causes = new HashMap<String, Class<? extends Cause>>();
    allCauses.forEach(
        it -> {
          @SuppressWarnings("unchecked")
          final var cause = (Class<? extends Cause>) it;
          causes.put(GroundTruthUtils.toShortString(cause), cause);
        });
    CAUSES = causes;
  }

  private static final Pattern COMMENT_PATTERN = Pattern.compile("//(.+)", CASE_INSENSITIVE);
  private static final Pattern SPEC_PATTERN =
      Pattern.compile("CC\\s*:\\s*([^;]*)\\s*", CASE_INSENSITIVE);
  private static final Pattern ENTRY_PATTERN =
      Pattern.compile(
          "^\\s*(\\[\\?]\\s*)?(\\w+)(?:\\s*/\\s*(\\w+))?(?:\\s+\"([^\"]+)\")?\\s*$",
          CASE_INSENSITIVE);

  public Map<Location, Set<GroundTruthEntry>> parse(final FileSystem fileSystem)
      throws IOException {
    final var parsedEntries = new HashMap<Location, Set<GroundTruthEntry>>();
    final var javaFiles = JavaFilesFinder.findJavaFiles(fileSystem);

    for (final var file : javaFiles) {
      parseLine(parsedEntries, file);
    }

    return parsedEntries;
  }

  private void parseLine(
      Map<Location, Set<GroundTruthEntry>> parsedEntries, final InputFile inputFile)
      throws IOException {
    final var lines = inputFile.contents().lines().toList();

    for (var line = 0; line < lines.size(); line++) {

      final var commentMatcher = COMMENT_PATTERN.matcher(lines.get(line));

      if (!commentMatcher.find()) {
        continue;
      }

      final var ccMatcher = SPEC_PATTERN.matcher(commentMatcher.group(1));

      if (!ccMatcher.find()
          || commentMatcher.group(1).substring(0, ccMatcher.start()).contains("//")) {
        continue;
      }

      sparseSpec(parsedEntries, inputFile, line + 1, ccMatcher.group(1));
    }
  }

  private void sparseSpec(
      Map<Location, Set<GroundTruthEntry>> parsedEntries,
      InputFile inputFile,
      int line,
      String spec)
      throws GroundTruthParsingException {
    final var entrySet = new HashSet<GroundTruthEntry>();

    for (final var specSplit : spec.split(",")) {
      final var entrymatcher = ENTRY_PATTERN.matcher(specSplit);

      if (!entrymatcher.find()) {
        throw new GroundTruthParsingException(
            String.format(
                "Invalid ground truth spec!\nFile: %s\nLine: %s", inputFile.filename(), line));
      }

      final var isOptional = entrymatcher.group(1) != null;
      final var rule = entrymatcher.group(2);
      final var cause = entrymatcher.group(3);
      final var value = entrymatcher.group(4);

      if (rule == null || cause == null) {
        throw new GroundTruthParsingException(
            String.format(
                "Invalid ground truth entry!\nFile: %s\nLine: %s", inputFile.filename(), line));
      }

      final RuleKind ruleKind;

      try {
        ruleKind = RuleKind.valueOf(rule);
      } catch (IllegalArgumentException _e) {
        throw new GroundTruthParsingException(
            String.format("Invalid rule kind!\nFile: %s\nLine: %s", inputFile.filename(), line));
      }

      final var causeType = CAUSES.get(cause);

      if (causeType == null) {
        throw new GroundTruthParsingException(
            String.format("Invalid cause type!\nFile: %s\nLine: %s", inputFile.filename(), line));
      }

      if (!entrySet.add(new GroundTruthEntry(ruleKind, causeType, value, isOptional)))
        throw new GroundTruthParsingException(
            String.format(
                "Duplicate ground truth entry!\nFile: %s\nLine: %s", inputFile.filename(), line));
    }

    parsedEntries.put(new Location(inputFile.filename(), line), entrySet);
  }

  public record Location(String fileName, int line) implements Comparable<Location> {
    @Override
    public int compareTo(Location o) {
      final var r1 = fileName.compareTo(o.fileName);
      if (r1 != 0) {
        return r1;
      }
      return Integer.compare(line, o.line);
    }

    @Override
    public String toString() {
      return fileName + " @ " + line;
    }
  }
}
