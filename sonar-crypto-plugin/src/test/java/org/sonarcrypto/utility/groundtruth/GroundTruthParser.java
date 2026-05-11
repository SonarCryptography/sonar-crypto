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
import org.sonarcrypto.ccerror.causes.InvalidValueCause;
import org.sonarcrypto.ccerror.causes.ValueCause;

@NullMarked
public class GroundTruthParser {
  private static final Map<String, Class<? extends Cause>> CAUSES;

  static {
    final var callCauses = Stream.of(CallCause.class.getPermittedSubclasses());
    final var valueCauses = Stream.of(ValueCause.class.getPermittedSubclasses());
    final var causes = Stream.concat(callCauses, valueCauses);

    final var trim = Cause.class.getSimpleName().length();
    final var causeMap = new HashMap<String, Class<? extends Cause>>();

    causes.forEach(
        it -> {
          final var simpleName = it.getSimpleName();
          //noinspection unchecked
          causeMap.put(
              simpleName.substring(0, simpleName.length() - trim), (Class<? extends Cause>) it);
        });
    CAUSES = causeMap;
  }

  private static final Pattern SPEC_PATTERN =
      Pattern.compile("//[^;]*?\\s*CC\\s*:\\s*([^;]*)", CASE_INSENSITIVE);
  private static final Pattern ENTRY_PATTERN =
      Pattern.compile("^\\s*(\\w+)(?:\\s*/\\s*(\\w+))?(\\s+\"([^\"]+)\")?\\s*$", CASE_INSENSITIVE);

  public Map<String, Map<Integer, Set<GroundTruthEntry>>> parse(final FileSystem fileSystem)
      throws IOException {
    final var fileMap = new HashMap<String, Map<Integer, Set<GroundTruthEntry>>>();
    final var javaFiles = JavaFilesFinder.findJavaFiles(fileSystem);

    for (final var file : javaFiles) {
      final var lineMap = parseLine(file);

      if (!lineMap.isEmpty()) fileMap.put(file.filename(), lineMap);
    }

    return fileMap;
  }

  private Map<Integer, Set<GroundTruthEntry>> parseLine(final InputFile inputFile)
      throws IOException {
    final var lineMap = new HashMap<Integer, Set<GroundTruthEntry>>();
    final var lines = inputFile.contents().split("\\r?\\n");

    for (var line = 0; line < lines.length; line++) {

      final var ccMatcher = SPEC_PATTERN.matcher(lines[line]);

      if (!ccMatcher.find()) continue;

      final var entrySet = sparseSpec(inputFile, line, ccMatcher.group(1));

      if (!entrySet.isEmpty()) lineMap.put(line, entrySet);
    }

    return lineMap;
  }

  private Set<GroundTruthEntry> sparseSpec(InputFile inputFile, int line, String spec) {
    final var entrySet = new HashSet<GroundTruthEntry>();

    for (final var specSplit : spec.split(",")) {
      final var entrymatcher = ENTRY_PATTERN.matcher(specSplit);

      if (!entrymatcher.find())
        throw new RuntimeException(
            String.format(
                "Invalid ground truth spec!\nFile: %s\nLine: %s", inputFile.filename(), line));

      final var rule = entrymatcher.group(1);
      final var cause = entrymatcher.group(2);
      final var value = entrymatcher.group(3);

      if (rule == null || cause == null)
        throw new RuntimeException(
            String.format(
                "Invalid ground truth entry!\nFile: %s\nLine: %s", inputFile.filename(), line));

      final RuleKind ruleKind;

      try {
        ruleKind = RuleKind.valueOf(rule);
      } catch (IllegalArgumentException _e) {
        throw new RuntimeException(
            String.format("Invalid rule kind!\nFile: %s\nLine: %s", inputFile.filename(), line));
      }

      final var causeType = CAUSES.get(cause);

      if (causeType == null)
        throw new RuntimeException(
            String.format("Invalid cause type!\nFile: %s\nLine: %s", inputFile.filename(), line));

      if (!entrySet.add(new GroundTruthEntry(ruleKind, InvalidValueCause.class, value)))
        throw new RuntimeException(
            String.format(
                "Duplicate ground truth entry!\nFile: %s\nLine: %s", inputFile.filename(), line));
    }

    return entrySet;
  }
}
