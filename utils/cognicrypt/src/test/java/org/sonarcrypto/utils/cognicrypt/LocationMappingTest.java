package org.sonarcrypto.utils.cognicrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

/**
 * Tests verifying that the line-number mapping in {@link JimpleConvertingView} correctly replaces
 * Jimple positions with original Java source positions when a {@code .map.json} sidecar file is
 * present, and gracefully falls back to Jimple positions when it is absent or invalid.
 */
class LocationMappingTest {

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /** Loads the JimpleTest class using the given jimple directory. */
  private JavaSootClass loadJimpleTestClass(Path jimpleDirectory) {
    JimpleAnalysisInputLocation inputLocation =
        new JimpleAnalysisInputLocation(
            jimpleDirectory, SourceType.Application, Collections.emptyList());
    JimpleConvertingView view = new JimpleConvertingView(inputLocation);
    ClassType type = view.getIdentifierFactory().getClassType("JimpleTest");
    Optional<JavaSootClass> opt = view.getClass(type);
    assertThat(opt).isPresent();
    return opt.get();
  }

  /** Returns the path to the standard test jimple directory (with map.json present). */
  private Path jimpleDirectoryWithMapping() {
    URL resource = getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple");
    assertThat(resource).isNotNull();
    return new File(resource.getFile()).getParentFile().toPath();
  }

  // -------------------------------------------------------------------------
  // Class-level position
  // -------------------------------------------------------------------------

  @Test
  void classPosition_isMappedToSourcePosition_whenMappingFilePresent() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    // CLASS mapping: firstLine=3, lastLine=40 (see JimpleTest.jimple.map.json)
    assertThat(clazz.getPosition().getFirstLine()).isEqualTo(3);
    assertThat(clazz.getPosition().getLastLine()).isEqualTo(40);
  }

  // -------------------------------------------------------------------------
  // Method-level positions
  // -------------------------------------------------------------------------

  @Test
  void defaultConstructorPosition_isMappedToSourcePosition() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    JavaSootMethod init =
        clazz.getMethods().stream()
            .filter(m -> m.getName().equals("<init>") && m.getParameterTypes().isEmpty())
            .findFirst()
            .orElseThrow();

    // METHOD mapping for <init>(): firstLine=7
    assertThat(init.getPosition().getFirstLine()).isEqualTo(7);
    assertThat(init.getPosition().getLastLine()).isEqualTo(10);
  }

  @Test
  void parameterizedConstructorPosition_isMappedToSourcePosition() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    JavaSootMethod init =
        clazz.getMethods().stream()
            .filter(m -> m.getName().equals("<init>") && !m.getParameterTypes().isEmpty())
            .findFirst()
            .orElseThrow();

    // METHOD mapping for <init>(int): firstLine=12
    assertThat(init.getPosition().getFirstLine()).isEqualTo(12);
    assertThat(init.getPosition().getLastLine()).isEqualTo(15);
  }

  @Test
  void getValueMethodPosition_isMappedToSourcePosition() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    JavaSootMethod getValue =
        clazz.getMethods().stream()
            .filter(m -> m.getName().equals("getValue"))
            .findFirst()
            .orElseThrow();

    // METHOD mapping for getValue(): firstLine=17
    assertThat(getValue.getPosition().getFirstLine()).isEqualTo(17);
    assertThat(getValue.getPosition().getLastLine()).isEqualTo(19);
  }

  @Test
  void setValueMethodPosition_isMappedToSourcePosition() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    JavaSootMethod setValue =
        clazz.getMethods().stream()
            .filter(m -> m.getName().equals("setValue"))
            .findFirst()
            .orElseThrow();

    // METHOD mapping for setValue(int): firstLine=21
    assertThat(setValue.getPosition().getFirstLine()).isEqualTo(21);
    assertThat(setValue.getPosition().getLastLine()).isEqualTo(23);
  }

  @Test
  void mainMethodPosition_isMappedToSourcePosition() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    JavaSootMethod main =
        clazz.getMethods().stream()
            .filter(m -> m.getName().equals("main"))
            .findFirst()
            .orElseThrow();

    // METHOD mapping for main: firstLine=25
    assertThat(main.getPosition().getFirstLine()).isEqualTo(25);
    assertThat(main.getPosition().getLastLine()).isEqualTo(30);
  }

  // -------------------------------------------------------------------------
  // Field-level positions
  // -------------------------------------------------------------------------

  @Test
  void fieldPosition_isMappedToSourcePosition() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    JavaSootField valueField =
        clazz.getFields().stream()
            .filter(f -> f.getName().equals("value"))
            .findFirst()
            .orElseThrow();

    // FIELD mapping: firstLine=5
    assertThat(valueField.getPosition().getFirstLine()).isEqualTo(5);
    assertThat(valueField.getPosition().getLastLine()).isEqualTo(5);
  }

  // -------------------------------------------------------------------------
  // Fallback: no map.json
  // -------------------------------------------------------------------------

  @Test
  void classPosition_fallsBackToJimplePosition_whenNoMappingFile(@TempDir Path tempDir)
      throws IOException {
    // Copy only the jimple file — no map.json
    URL jimpleResource = getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple");
    assertThat(jimpleResource).isNotNull();
    Files.copy(Path.of(jimpleResource.getFile()), tempDir.resolve("JimpleTest.jimple"));

    JavaSootClass clazz = loadJimpleTestClass(tempDir);

    // Without mapping the position is the Jimple-file position (line 0 — SootUp uses 0 for class)
    assertThat(clazz.getPosition().getFirstLine()).isEqualTo(0);
  }

  @Test
  void methodPositions_fallBackToJimplePositions_whenNoMappingFile(@TempDir Path tempDir)
      throws IOException {
    URL jimpleResource = getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple");
    assertThat(jimpleResource).isNotNull();
    Files.copy(Path.of(jimpleResource.getFile()), tempDir.resolve("JimpleTest.jimple"));

    JavaSootClass clazz = loadJimpleTestClass(tempDir);

    // All methods should still be present even without a mapping
    assertThat(clazz.getMethods()).isNotEmpty();
    clazz
        .getMethods()
        .forEach(
            m ->
                // Positions should be valid (non-negative line numbers)
                assertThat(m.getPosition().getFirstLine()).isGreaterThanOrEqualTo(0));
  }

  @Test
  void fieldPosition_fallsBackToJimplePosition_whenNoMappingFile(@TempDir Path tempDir)
      throws IOException {
    URL jimpleResource = getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple");
    assertThat(jimpleResource).isNotNull();
    Files.copy(Path.of(jimpleResource.getFile()), tempDir.resolve("JimpleTest.jimple"));

    JavaSootClass clazz = loadJimpleTestClass(tempDir);

    JavaSootField valueField =
        clazz.getFields().stream()
            .filter(f -> f.getName().equals("value"))
            .findFirst()
            .orElseThrow();

    // Without mapping the position reflects the raw Jimple line (SootUp assigns line 1 for the
    // field)
    assertThat(valueField.getPosition().getFirstLine()).isEqualTo(1);
    // Most importantly, it should NOT be the mapped source line (5)
    assertThat(valueField.getPosition().getFirstLine()).isNotEqualTo(5);
  }

  // -------------------------------------------------------------------------
  // Fallback: malformed map.json
  // -------------------------------------------------------------------------

  @Test
  void view_doesNotThrow_whenMappingFileIsMalformed(@TempDir Path tempDir) throws IOException {
    URL jimpleResource = getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple");
    assertThat(jimpleResource).isNotNull();
    Files.copy(Path.of(jimpleResource.getFile()), tempDir.resolve("JimpleTest.jimple"));
    Files.writeString(tempDir.resolve("JimpleTest.jimple.map.json"), "{ this is not valid json }");

    // Should not throw — invalid map.json is logged as a warning and ignored
    JavaSootClass clazz = loadJimpleTestClass(tempDir);

    assertThat(clazz).isNotNull();
    assertThat(clazz.getMethods()).isNotEmpty();
    assertThat(clazz.getFields()).isNotEmpty();
  }

  @Test
  void classPosition_fallsBackToJimplePosition_whenMappingFileIsMalformed(@TempDir Path tempDir)
      throws IOException {
    URL jimpleResource = getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple");
    assertThat(jimpleResource).isNotNull();
    Files.copy(Path.of(jimpleResource.getFile()), tempDir.resolve("JimpleTest.jimple"));
    Files.writeString(tempDir.resolve("JimpleTest.jimple.map.json"), "{ this is not valid json }");

    JavaSootClass clazz = loadJimpleTestClass(tempDir);

    // Falls back to Jimple line 0 (SootUp assigns 0 as the class position)
    assertThat(clazz.getPosition().getFirstLine()).isEqualTo(0);
  }

  // -------------------------------------------------------------------------
  // Mapping consistency
  // -------------------------------------------------------------------------

  @Test
  void allMappedMethodPositions_areDistinctSourceLines() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    // Each method in the map has a unique source firstLine
    long distinctFirstLines =
        clazz.getMethods().stream()
            .mapToInt(m -> m.getPosition().getFirstLine())
            .distinct()
            .count();

    assertThat(distinctFirstLines).isEqualTo(clazz.getMethods().size());
  }

  @Test
  void mappedPositions_areStrictlyPositiveLineNumbers() {
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    clazz
        .getMethods()
        .forEach(
            m ->
                assertThat(m.getPosition().getFirstLine())
                    .as("Method %s should have a positive source line", m.getName())
                    .isPositive());

    clazz
        .getFields()
        .forEach(
            f ->
                assertThat(f.getPosition().getFirstLine())
                    .as("Field %s should have a positive source line", f.getName())
                    .isPositive());

    assertThat(clazz.getPosition().getFirstLine())
        .as("Class position should have a positive source line")
        .isPositive();
  }

  @Test
  void mappedSourcePositions_areEarlierThanJimpleLineNumbers() {
    // The mapped Java source lines should generally be lower than the corresponding
    // Jimple output lines since Jimple expands each method body.
    JavaSootClass clazz = loadJimpleTestClass(jimpleDirectoryWithMapping());

    // Methods: jimple lines 3,11,21,29,38 → source lines 7,12,17,21,25 — all lower than jimple
    JavaSootMethod main =
        clazz.getMethods().stream()
            .filter(m -> m.getName().equals("main"))
            .findFirst()
            .orElseThrow();

    // Jimple line 38 mapped to source line 25
    assertThat(main.getPosition().getFirstLine()).isEqualTo(25);
    assertThat(main.getPosition().getFirstLine()).isLessThan(38);
  }
}
