package org.sonarcrypto.utils.maven;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MavenProjectTest {

  private static final String MAVEN_PROJECT_PATH =
      FilenameUtils.normalize(
          new File("../../e2e/src/test/resources/Java/Maven/Basic").getAbsolutePath());

  @Test
  void mavenProjectTest() throws Exception {
    final var mavenProject = new MavenProject(MAVEN_PROJECT_PATH);

    mavenProject.compile();

    final var classPath = mavenProject.getBuildDirectory();
    final var jimplePath = mavenProject.getJimpleDirectory();
    final var fullClassPath = mavenProject.getFullClassPath();

    final var buildDirectory =
        MAVEN_PROJECT_PATH + File.separator + "target" + File.separator + "classes";
    final var jimpleDirectory =
        MAVEN_PROJECT_PATH + File.separator + "target" + File.separator + "jimple";

    assertEquals(buildDirectory, classPath);
    assertEquals(jimpleDirectory, jimplePath);
    assertNotNull(fullClassPath);

    assertTrue(new File(buildDirectory).exists());
    assertTrue(new File(jimpleDirectory).exists());
  }

  @Test
  void mavenProjectErr1Test() throws Exception {
    final var mavenProject = new MavenProject(MAVEN_PROJECT_PATH);
    Assertions.assertThrows(IllegalStateException.class, mavenProject::getBuildDirectory);
  }

  @Test
  void mavenProjectErr2Test() throws Exception {
    final var mavenProject = new MavenProject(MAVEN_PROJECT_PATH);
    Assertions.assertThrows(IllegalStateException.class, mavenProject::getJimpleDirectory);
  }

  @Test
  void mavenProjectErr3Test() throws Exception {
    final var mavenProject = new MavenProject(MAVEN_PROJECT_PATH);
    Assertions.assertThrows(IllegalStateException.class, mavenProject::getFullClassPath);
  }

  @Test
  void mavenProjectErr4Test() {
    Assertions.assertThrows(
        FileNotFoundException.class, () -> new MavenProject("a_non_existing_folder"));
  }
}
