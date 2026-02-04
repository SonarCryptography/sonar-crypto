package org.sonarcrypto.jbc2jimple;

import boomerang.scope.sootup.BoomerangPreInterceptor;
import soot.options.Options;
import sootup.core.model.SourceType;
import sootup.core.util.printer.JimplePrinter;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardOpenOption.*;

public class Jbc2JimpleConverter {
	public void convert(
		String javaClassPath,
		String jimpleOutputPath
	) throws IOException {
		Options.v().set_allow_phantom_refs(true);
		
		final var javaView = new JavaView(new JavaClassPathAnalysisInputLocation(
			javaClassPath,
			SourceType.Application,
			List.of(new BoomerangPreInterceptor())
		));
		
		final var jimplePrinter = new JimplePrinter();
		
		final var outputDir = Path.of(jimpleOutputPath);
		final var sootClassesIterator = javaView.getClasses().iterator();
		
		while(sootClassesIterator.hasNext()) {
			final var sootClass = sootClassesIterator.next();
			
			try(final var out = new PrintWriter(Files.newOutputStream(
				outputDir.resolve(sootClass.getName() + ".jimple"),
				CREATE, WRITE
			))) {
				jimplePrinter.printTo(sootClass, out);
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length < 2) {
			System.err.println(
				"Invalid command line arguments. Expected Java class path and Jimple output path."
			);
			System.exit(1);
		}
		
		new Jbc2JimpleConverter().convert(args[0], args[1]);
	}
}
