package org.sonarcrypto;

import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import java.util.Locale;
import java.util.concurrent.Callable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public final class CliArgs implements Callable<Integer> {
  @Option(
      names = {"-cp", "--classPath"},
      description = "Class path")
  private String classPath = null;

  @Option(
      names = {"-mvn", "--mvnProject"},
      description = "Path to maven project")
  private String mvnProject = null;

  @Option(
      names = {"-rs", "--ruleset"},
      required = true,
      description = "Ruleset name, e.g. jca")
  private String ruleset = null;

  @Option(
      names = {"-f", "--framework"},
      defaultValue = "soot",
      description = "Analysis framework (soot or sootup)")
  private String framework = null;

  private CliArgs() {}

  /** Gets the class path. This is an optional argument. */
  public @Nullable String getClassPath() {
    return classPath;
  }

  /** Gets the Maven project path. This is an optional argument. */
  public @Nullable String getMvnProject() {
    return mvnProject;
  }

  /** Gets the ruleset name. This is a required argument. */
  public @NonNull String getRuleset() {
    return ruleset;
  }

  /**
   * Gets the analysis framework. This is an optional argument with the default value {@link
   * Framework#SOOT SOOT}.
   */
  public @NonNull Framework getFramework() {
    return switch (framework.toLowerCase(Locale.ROOT)) {
      case "soot" -> Framework.SOOT;
      case "sootup" -> Framework.SOOT_UP;
      default -> throw new RuntimeException("Unsupported framework: " + framework);
    };
  }

  /**
   * Parses the command line arguments.
   *
   * @param args The command line arguments to parse.
   * @return The parsed {@link CliArgs}.
   */
  public static @NonNull CliArgs parse(@NonNull String @NonNull [] args) {
    final var cliArgs = new CliArgs();

    final var parser = new CommandLine(cliArgs);
    parser.setOptionsCaseInsensitive(true);

    if (parser.execute(args) != CommandLine.ExitCode.OK)
      throw new RuntimeException("Failed parsing CLI arguments!");

    return cliArgs;
  }

  @Override
  public @NonNull Integer call() {
    return 0;
  }
}
