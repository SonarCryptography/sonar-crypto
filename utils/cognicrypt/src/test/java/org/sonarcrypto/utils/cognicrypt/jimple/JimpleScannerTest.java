package org.sonarcrypto.utils.cognicrypt.jimple;

import static org.assertj.core.api.Assertions.assertThat;

import de.fraunhofer.iem.scanner.ScannerSettings;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class JimpleScannerTest {

  @Test
  void constructor_and_setAddClassPath() throws Exception {
    var scanner = new JimpleScanner("application-path", "ruleset-path");
    scanner.setAddClassPath("dep-a" + java.io.File.pathSeparator + "dep-b");

    var settings = (ScannerSettings) getField(scanner, "settings");

    assertThat(settings.getApplicationPath()).isEqualTo("application-path");
    assertThat(settings.getRulesetPath()).isEqualTo("ruleset-path");
    assertThat(settings.getCallGraph()).isEqualTo(ScannerSettings.CallGraphAlgorithm.RTA);
    assertThat(settings.getAddClassPath())
        .isEqualTo("dep-a" + java.io.File.pathSeparator + "dep-b");
  }

  private static Object getField(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }
}
