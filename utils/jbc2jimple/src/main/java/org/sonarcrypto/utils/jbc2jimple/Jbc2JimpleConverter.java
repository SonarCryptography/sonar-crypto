package org.sonarcrypto.utils.jbc2jimple;

import boomerang.scope.sootup.BoomerangPreInterceptor;
import org.jspecify.annotations.NullMarked;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import sootup.core.model.SourceType;
import sootup.core.util.printer.JimplePrinter;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static java.nio.file.StandardOpenOption.*;

@SuppressWarnings("ClassCanBeRecord")
@NullMarked
public class Jbc2JimpleConverter {
	
	private final boolean isBoomerangPreInterceptorEnabled;
	
	/**
	 * Creates a new instance with <i>disabled</i> BoomerangPreInterceptor.
	 * <p>
	 * To enable the BoomerangPreInterceptor,
	 * use the overloaded constructor {@link #Jbc2JimpleConverter(boolean)}.
	 * <p>
	 * Example:
	 * <p>
	 * <pre><code>
	 * new Jbc2JimpleConverter().convert("/java/class/path", "/jimple/output/directory");
	 * </code></pre>
	 */
	public Jbc2JimpleConverter() {
		this(false);
	}
	
	/**
	 * Creates a new instance.
	 * <p>
	 * To use the default setting for the BoomerangPreInterceptor,
	 * use the parameterless constructor {@link #Jbc2JimpleConverter()}.
	 * <p>
	 * Example:
	 * <p>
	 * <pre><code>
	 * new Jbc2JimpleConverter(true).convert("/java/class/path", "/jimple/output/directory");
	 * </code></pre>
	 *
	 * @param enableBoomerangPreInterceptor Set to `true` to enable the BoomerangPreInterceptor.
	 */
	public Jbc2JimpleConverter(final boolean enableBoomerangPreInterceptor) {
		isBoomerangPreInterceptorEnabled = enableBoomerangPreInterceptor;
	}
	
	/**
	 * Gets a value indicating whether the BoomerangPreInterceptor is enabled.
	 */
	public boolean isBoomerangPreInterceptorEnabled() {
		return isBoomerangPreInterceptorEnabled;
	}
	
	/**
	 * Converts Java classes of the given class path into Jimple files
	 * that are written into the given output directory.
	 * <p>
	 * Example:
	 * <p>
	 * <pre><code>
	 * new Jbc2JimpleConverter().convert("/java/class/path", "/jimple/output/directory");
	 * </code></pre>
	 *
	 * @param javaClassPath The Java class path.
	 * @param jimpleOutputDirectory The Jimple output directory.
	 * @return The number of converted classes.
	 * @throws IOException An I/O exception occurred.
	 */
	public long convert(
		String javaClassPath,
		String jimpleOutputDirectory
	) throws IOException {
		final var jimpleOutputPath = Path.of(jimpleOutputDirectory);
		
		if(!Files.exists(jimpleOutputPath))
			Files.createDirectories(jimpleOutputPath);
		else if(!Files.isDirectory(jimpleOutputPath))
			throw new IOException("The Jimple output directory is not a directory.");
		
		final var javaView = new JavaView(new JavaClassPathAnalysisInputLocation(
			javaClassPath,
			SourceType.Application,
			isBoomerangPreInterceptorEnabled() ? List.of(new BoomerangPreInterceptor()) : List.of()
		));
		
		final var jimplePrinter = new JimplePrinter();
		final var sootClassesIterator = javaView.getClasses().iterator();
		
		var convertedClasses = 0L;
		
		while(sootClassesIterator.hasNext()) {
			final var sootClass = sootClassesIterator.next();
			
			try(final var out = new PrintWriter(Files.newOutputStream(
				jimpleOutputPath.resolve(sootClass.getName() + ".jimple"),
				CREATE, WRITE, TRUNCATE_EXISTING
			))) {
				jimplePrinter.printTo(sootClass, out);
				convertedClasses++;
			}
		}
		
		return convertedClasses;
	}
	
	@SuppressWarnings("DataFlowIssue")
	@Command(mixinStandardHelpOptions = true)
	private static class CliArgs implements Callable<Integer> {
		
		@Option(
			names = {"-bpi", "--enableBoomerangPreInterceptor"},
			description = "Enables the BoomerangPreInterceptor.")
		private boolean enableBoomerangPreInterceptor = false;
		
		@Option(
			names = {"-cp", "--classPath"},
			description = "Sets the class path",
			required = true)
		private String classPath = null;
		
		@Option(
			names = {"-jo", "--jimpleOutput"},
			description = "Sets the Jimple output directory",
			required = true)
		private String outputPath = null;
		
		@Override
		public Integer call() {
			return 0;
		}
	}
	
	/**
	 * Runs the JBC-to-Jimple converter from the command line.
	 * <p>
	 * Example arguments:
	 * <p>
	 * <pre>
	 * -classPath /java/class/path -jimpleOutput /jimple/output/directory -enableBoomerangPreInterceptor
	 * 
	 * -cp /java/class/path        -jo /jimple/output/directory           -bpi
	 * </pre>
	 */
	public static void main(String[] args) throws IOException {
		final var cliArgs = new CliArgs();
		CommandLine parser = new CommandLine(cliArgs);
		parser.setOptionsCaseInsensitive(true);
		
		if(parser.execute(args) != ExitCode.OK) {
			System.err.println("Error while parsing the CLI arguments");
			System.exit(1);
		}
		
		System.out.println("Java class path:         " + cliArgs.classPath);
		System.out.println("Jimple output directory: " + cliArgs.outputPath);
		
		if(cliArgs.enableBoomerangPreInterceptor) {
			System.out.println();
			System.out.println("BoomerangPreInterceptor enabled");
		}
		
		System.out.println();
		System.out.println("Converting classes ...");
		
		final var count = new Jbc2JimpleConverter(cliArgs.enableBoomerangPreInterceptor)
			.convert(cliArgs.classPath, cliArgs.outputPath);
		
		System.out.println();
		System.out.println("Done. " + count + " class file(s) converted.");
	}
}
