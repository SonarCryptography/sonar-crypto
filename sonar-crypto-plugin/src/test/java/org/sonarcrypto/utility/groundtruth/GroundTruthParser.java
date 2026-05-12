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
          causes.put(GroundTruthUtils.toString(cause), cause);
        });
    CAUSES = causes;
  }

  private static final Pattern COMMENT_PATTERN = Pattern.compile("//(.+)", CASE_INSENSITIVE);
  private static final Pattern SPEC_PATTERN =
      Pattern.compile("CC\\s*:\\s*([^;]*)\\s*", CASE_INSENSITIVE);
  private static final Pattern ENTRY_PATTERN =
      Pattern.compile(
          "^\\s*(\\w+)(?:\\s*/\\s*(\\w+))?(?:\\s+\"([^\"]+)\")?\\s*$", CASE_INSENSITIVE);

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
    final var lines = inputFile.contents().split("\\r?\\n");

    for (var line = 0; line < lines.length; line++) {

      final var commentMatcher = COMMENT_PATTERN.matcher(lines[line]);

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

      final var rule = entrymatcher.group(1);
      final var cause = entrymatcher.group(2);
      final var value = entrymatcher.group(3);

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

      if (!entrySet.add(new GroundTruthEntry(ruleKind, causeType, value)))
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
