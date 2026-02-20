package org.sonarcrypto.utils.test.runner;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;
import org.sonarcrypto.utils.maven.MavenBuildException;
import org.sonarcrypto.utils.maven.MavenProject;

@NullMarked
public non-sealed class MavenProjectTestRunner extends TestRunner<MavenProjectTestRunner.Result> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MavenProjectTestRunner.class);

  private final ClassPathTestRunner classPathTestRunner = new ClassPathTestRunner();

  /**
   * Runs the analysis.
   *
   * @param path The maven project path.
   * @param ruleset The ruleset.
   * @return The analysis result.
   * @throws IOException An I/O error is occurred.
   */
  @Override
  public Result run(final String path, final Ruleset ruleset)
      throws IOException, URISyntaxException {
    final var mavenProjectPath = new File(path).getAbsolutePath();
    final String classPath;

    MavenProject mavenProject;

    try {
      mavenProject = new MavenProject(mavenProjectPath);
      mavenProject.compile();
      classPath = mavenProject.getBuildDirectory();
      LOGGER.info("Built project to directory: {}", classPath);
    } catch (MavenBuildException e) {
      LOGGER.error("Failed to build project", e);
      System.exit(1);
      throw new Error();
    }

    LOGGER.info("Maven project: {}", classPath);

    final var analysisResult = classPathTestRunner.run(classPath, ruleset);

    return new Result(mavenProject, analysisResult);
  }

  public record Result(
      MavenProject mavenProject, Table<WrappedClass, Method, Set<AbstractError>> collectedErrors) {}
}
