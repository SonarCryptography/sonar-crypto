package org.sonarcrypto.utils.cognicrypt.jimple;

import static org.assertj.core.api.Assertions.assertThat;

import de.fraunhofer.iem.scanner.ScannerSettings;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import sootup.core.views.View;

class JimpleFrameworkSetupTest {

  @Test
  void initializeFramework_withIncludeJdk_loadsRuntimeClasses() throws Exception {
    var jimpleDirectory =
        Path.of(
                Objects.requireNonNull(
                        getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple"))
                    .toURI())
            .getParent()
            .toString();
    var setup =
        new JimpleFrameworkSetup(
            jimpleDirectory, ScannerSettings.CallGraphAlgorithm.RTA, null, true);

    setup.initializeFramework();

    var view = (View) getField(setup, "view");
    var stringType = view.getIdentifierFactory().getClassType("java.lang.String");

    assertThat(view.getClass(stringType)).isPresent();
  }

  private static Object getField(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }
}
