package org.sonarcrypto.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class MavenProjectTest {

  private static final String BASIC_PROJECT = "../e2e/src/test/resources/Java/Maven/Basic";

  @Test
  void compile() throws Exception {
    var project = new MavenProject(BASIC_PROJECT);

    project.compile();

    assertThat(Paths.get(project.getBuildDirectory())).isDirectory();
    assertThat(Paths.get(project.getJimpleDirectory())).isDirectory();
    assertThat(project.getFullClassPath()).isNotBlank();
  }

  @Test
  void constructor_throws_for_non_existent_path() {
    assertThatThrownBy(() -> new MavenProject("/non/existent/path"))
        .isInstanceOf(FileNotFoundException.class);
  }
}
