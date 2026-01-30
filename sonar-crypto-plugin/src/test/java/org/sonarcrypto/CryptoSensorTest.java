package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.TypestateError;
import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import org.jspecify.annotations.NullMarked;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarcrypto.runner.ClassPathTestRunner;
import org.sonarcrypto.runner.MavenProjectTestRunner;

@NullMarked
class CryptoSensorTest {
	@RegisterExtension LogTesterJUnit5 logTester = new LogTesterJUnit5();
	
	@TempDir Path tempDir;
	
	@Test
	void fails_to_build_for_non_maven_project_dir() {
		CryptoSensor sensor = new CryptoSensor();
		sensor.execute(SensorContextTester.create(tempDir));
		assertThat(logTester.logs()).containsExactly("Failed to build Maven project");
	}
	
	@Test
	void mavenProjectTest() throws IOException {
		final var runner = new MavenProjectTestRunner(Framework.SOOT_UP);
		final var errors = runner.run("../testProjects/test", Ruleset.JCA);
		final var errorSet = errors.cellSet();
		
		System.out.println();
		
		Assert.assertFalse(errors.isEmpty());
		
		for(final var wrappedClassMethodSetCell : errorSet) {
			
			Assert.assertEquals(
				"org.sonarcrypto.test.App",
				wrappedClassMethodSetCell.getRowKey().getFullyQualifiedName()
			);
			
			Assert.assertEquals(
				"void main(java.lang.String[])",
				wrappedClassMethodSetCell.getColumnKey().getSubSignature()
			);
			
			Assert.assertEquals(2, wrappedClassMethodSetCell.getValue().size());
			
			for(final var setValue : wrappedClassMethodSetCell.getValue()) {
				
				Assert.assertTrue(
					setValue instanceof ConstraintError || setValue instanceof TypestateError
				);
			}
			
			System.out.println(wrappedClassMethodSetCell);
			System.out.println();
		}
	}
}
