package org.sonarcrypto;

import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Locale;
import java.util.concurrent.Callable;

@Command(mixinStandardHelpOptions = true)
public final class CliArgs implements Callable<Integer> {
	@Option(names = { "-cp", "--classPath"}, description = "Class path")
	private String classPath = null;
	
	@Option(names = { "-mvn", "--mvnProject"}, description = "Path to maven project")
	private String mvnProject = null;
	
	@Option(names = { "-rs", "--ruleset"}, required = true, description = "Ruleset name, e.g. jca")
	private String ruleset = null;
	
	@Option(
		names = { "-f", "--framework"},
		defaultValue = "soot",
		description = "Analysis framework (soot or sootup)"
	)
	private String framework = null;
	
	private CliArgs() { }
	
	public String getClassPath() {
		return classPath;
	}
	
	public String getMvnProject() {
		return mvnProject;
	}
	
	public String getRuleset() {
		return ruleset;
	}
	
	public Framework getFramework() {
		return switch(framework.toLowerCase(Locale.ROOT)) {
			case "soot" -> Framework.SOOT;
			case "sootup" -> Framework.SOOT_UP;
			default -> throw new RuntimeException("Unsupported framework: " + framework);
		};
	}
	
	public static CliArgs parse(String[] args) {
		final var cliArgs = new CliArgs();
		CommandLine parser = new CommandLine(cliArgs);
		parser.setOptionsCaseInsensitive(true);
		
		if(parser.execute(args) != CommandLine.ExitCode.OK)
			throw new RuntimeException("Failed parsing CLI arguments!");
		
		return cliArgs;
	}
	
	@Override
	public Integer call() {
		return 0;
	}
}
