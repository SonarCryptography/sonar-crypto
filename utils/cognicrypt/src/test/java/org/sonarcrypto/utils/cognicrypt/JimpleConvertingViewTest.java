package org.sonarcrypto.utils.cognicrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.java.bytecode.frontend.inputlocation.DefaultRuntimeAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

class JimpleConvertingViewTest {

  @Test
  void readJimpleFromResources_createsView() {
    JimpleAnalysisInputLocation inputLocation = getAnalysisInputLocationForTestJimple();

    // Create the JimpleConvertingView
    JimpleConvertingView view = new JimpleConvertingView(inputLocation);

    assertThat(view).isNotNull();
    assertThat(view).isInstanceOf(JavaView.class);
  }

  @Test
  void readJimpleFromResources_loadsClass() {
    JimpleAnalysisInputLocation inputLocation = getAnalysisInputLocationForTestJimple();

    // Create the view
    JimpleConvertingView view = new JimpleConvertingView(inputLocation);

    // Verify the class was loaded
    long classCount = view.getClasses().count();
    assertThat(classCount).isGreaterThan(0);
  }

  @Test
  void readJimpleFromResources_retrievesSpecificClass() {
    JimpleAnalysisInputLocation inputLocation = getAnalysisInputLocationForTestJimple();

    // Create the view
    JimpleConvertingView view = new JimpleConvertingView(inputLocation);

    // Get the JimpleTest class
    ClassType jimpleTestType = view.getIdentifierFactory().getClassType("JimpleTest");

    assertThat(view.getClass(jimpleTestType)).isPresent();

    Optional<JavaSootClass> jimpleTestClassOptional = view.getClass(jimpleTestType);
    assertThat(jimpleTestClassOptional).isPresent();
    JavaSootClass jimpleTestClass = jimpleTestClassOptional.get();
    assertThat(jimpleTestClass).isNotNull();
    assertThat(jimpleTestClass.getType().getClassName()).isEqualTo("JimpleTest");
  }

  @Test
  void readJimpleFromResources_hasExpectedMethods() {
    JimpleAnalysisInputLocation inputLocation = getAnalysisInputLocationForTestJimple();

    // Create the view
    JimpleConvertingView view = new JimpleConvertingView(inputLocation);
    view.getClasses();
    // Get the JimpleTest class
    ClassType jimpleTestType = view.getIdentifierFactory().getClassType("JimpleTest");

    Optional<JavaSootClass> jimpleTestClassOptional = view.getClass(jimpleTestType);
    assertThat(jimpleTestClassOptional).isPresent();
    JavaSootClass jimpleTestClass = jimpleTestClassOptional.get();

    // Verify the class has expected methods
    assertThat(jimpleTestClass.getMethods()).isNotEmpty();

    // Check for specific methods by name
    long initMethodCount =
        jimpleTestClass.getMethods().stream().filter(m -> m.getName().equals("<init>")).count();
    assertThat(initMethodCount).isEqualTo(2); // Two constructors

    boolean hasGetValue =
        jimpleTestClass.getMethods().stream().anyMatch(m -> m.getName().equals("getValue"));
    assertThat(hasGetValue).isTrue();

    boolean hasSetValue =
        jimpleTestClass.getMethods().stream().anyMatch(m -> m.getName().equals("setValue"));
    assertThat(hasSetValue).isTrue();

    boolean hasMain =
        jimpleTestClass.getMethods().stream().anyMatch(m -> m.getName().equals("main"));
    assertThat(hasMain).isTrue();
  }

  @Test
  void readJimpleFromResources_hasExpectedField() {
    JimpleAnalysisInputLocation inputLocation = getAnalysisInputLocationForTestJimple();

    // Create the view
    JimpleConvertingView view = new JimpleConvertingView(inputLocation);

    // Get the JimpleTest class
    ClassType jimpleTestType = view.getIdentifierFactory().getClassType("JimpleTest");
    Optional<JavaSootClass> jimpleTestClassOptional = view.getClass(jimpleTestType);
    assertThat(jimpleTestClassOptional).isPresent();
    JavaSootClass jimpleTestClass = jimpleTestClassOptional.get();

    // Verify the class has the value field
    assertThat(jimpleTestClass.getFields()).isNotEmpty();

    boolean hasValueField =
        jimpleTestClass.getFields().stream().anyMatch(f -> f.getName().equals("value"));
    assertThat(hasValueField).isTrue();
  }

  @Test
  void readJimpleWithRuntimeInputLocation_createsViewWithMultipleLocations() {
    // Create first input location (Jimple)
    JimpleAnalysisInputLocation jimpleInputLocation = getAnalysisInputLocationForTestJimple();

    // Create second input location (Runtime/JDK classes)
    AnalysisInputLocation runtimeInputLocation = new DefaultRuntimeAnalysisInputLocation();

    // Create the view with both input locations
    List<AnalysisInputLocation> inputLocations = List.of(jimpleInputLocation, runtimeInputLocation);
    JimpleConvertingView view = new JimpleConvertingView(inputLocations);

    assertThat(view).isNotNull();
    assertThat(view).isInstanceOf(JavaView.class);

    // Verify we can access both Jimple classes and runtime classes
    ClassType jimpleTestType = view.getIdentifierFactory().getClassType("JimpleTest");
    assertThat(view.getClass(jimpleTestType)).isPresent();

    // Verify we can access JDK classes from the runtime input location
    ClassType stringType = view.getIdentifierFactory().getClassType("java.lang.String");
    assertThat(view.getClass(stringType)).isPresent();

    // Verify we can access Object class from runtime
    ClassType objectType = view.getIdentifierFactory().getClassType("java.lang.Object");
    assertThat(view.getClass(objectType)).isPresent();
  }

  private JimpleAnalysisInputLocation getAnalysisInputLocationForTestJimple() {
    URL resource = getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple");
    assertThat(resource).isNotNull();

    File jimpleFile = new File(resource.getFile());
    Path jimpleDirectory = jimpleFile.getParentFile().toPath();

    return new JimpleAnalysisInputLocation(
        jimpleDirectory, SourceType.Application, Collections.emptyList());
  }
}
