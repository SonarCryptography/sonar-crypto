package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

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
	void example_test() throws IOException {
		var runner = new CogniCryptTestRunner(Framework.SOOT);
		var errors = runner.run("", Ruleset.JCA);
		var errorSet = errors.cellSet();
		
		for(var wrappedClassMethodSetCell : errorSet) {
			System.out.println(wrappedClassMethodSetCell);
		}
	}
}
