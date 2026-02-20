/********************************************************************************
 * Copyright (c) 2017 Fraunhofer IEM, Paderborn, Germany
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.sonarcrypto.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.maven.api.cli.ExecutorException;
import org.apache.maven.api.cli.ExecutorRequest;
import org.apache.maven.cling.executor.forked.ForkedMavenExecutor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.utils.jbc2jimple.Jbc2JimpleConverter;

@NullMarked
public class MavenProject {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenProject.class);
  private final String pathToProjectRoot;
  private boolean compiled;
  private @Nullable String fullProjectClassPath;

  public MavenProject(String pathToProjectRoot) throws FileNotFoundException {
    File file = new File(pathToProjectRoot);
    if (!file.exists()) {
      throw new FileNotFoundException("The path " + pathToProjectRoot + " does not exist!");
    }
    this.pathToProjectRoot = new File(pathToProjectRoot).getAbsolutePath();
  }

  private static Path resolveMavenHome() throws MavenBuildException {
    String mavenHome = System.getProperty("maven.home");
    if (mavenHome == null) {
      mavenHome = System.getenv("MAVEN_HOME");
    }
    if (mavenHome != null) {
      return Paths.get(mavenHome);
    }
    return findMvnOnPath();
  }

  private static Path findMvnOnPath() throws MavenBuildException {
    boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("windows");
    String[] mvnNames = isWindows ? new String[] {"mvn.cmd", "mvn"} : new String[] {"mvn"};
    String pathEnv = System.getenv("PATH");
    if (pathEnv != null) {
      for (String dir : pathEnv.split(File.pathSeparator)) {
        for (String mvnName : mvnNames) {
          Path candidate = Paths.get(dir, mvnName);
          if (Files.isExecutable(candidate)) {
            try {
              return candidate.toRealPath().getParent().getParent();
            } catch (IOException e) {
              // Skip candidates that can't be resolved
            }
          }
        }
      }
    }
    throw new MavenBuildException(
        "Cannot find Maven installation. Set maven.home system property, MAVEN_HOME environment variable, or ensure mvn is on the PATH.");
  }

  public void compile() throws MavenBuildException {
    var request =
        ExecutorRequest.mavenBuilder(resolveMavenHome())
            .cwd(Paths.get(pathToProjectRoot))
            .arguments(List.of("clean", "compile"))
            .build();

    try (ForkedMavenExecutor executor = new ForkedMavenExecutor()) {
      int exitCode = executor.execute(request);
      if (exitCode != 0) {
        throw new MavenBuildException("Was not able to compile project " + pathToProjectRoot + ".");
      }
    } catch (ExecutorException e) {
      throw new MavenBuildException(
          "Was not able to invoke maven in path " + pathToProjectRoot + ". Does a pom.xml exist?",
          e);
    }
    compiled = true;
    computeClassPath();
    buildJimple();
  }

  private void computeClassPath() throws MavenBuildException {
    var request =
        ExecutorRequest.mavenBuilder(resolveMavenHome())
            .cwd(Paths.get(pathToProjectRoot))
            .arguments(List.of("dependency:build-classpath", "-Dmdep.outputFile=classPath.temp"))
            .build();

    try (ForkedMavenExecutor executor = new ForkedMavenExecutor()) {
      int exitCode = executor.execute(request);
      if (exitCode != 0) {
        throw new MavenBuildException(
            "Was not able to compute dependencies " + pathToProjectRoot + ".");
      }
    } catch (ExecutorException e) {
      throw new MavenBuildException("Was not able to invoke maven to compute dependencies", e);
    }
    try {
      File classPathFile = new File(pathToProjectRoot + File.separator + "classPath.temp");
      try (var in = new FileInputStream(classPathFile)) {
        fullProjectClassPath = IOUtils.toString(in, StandardCharsets.UTF_8);
      }
      if (!classPathFile.delete()) {
        LOGGER.warn(
            "Failed to delete temporary classpath file: {}", classPathFile.getAbsolutePath());
      }
    } catch (IOException e) {
      throw new MavenBuildException(
          "Was not able to read in class path from file classPath.temp", e);
    }
  }

  public String getBuildDirectory() {
    if (!compiled) {
      throw new IllegalStateException(
          "You first have to compile the project. Use method compile()");
    }
    return pathToProjectRoot + File.separator + "target" + File.separator + "classes";
  }

  public @Nullable String getFullClassPath() {
    if (!compiled) {
      throw new IllegalStateException("Project has not been compiled yet.");
    }
    return fullProjectClassPath;
  }

  public String getJimpleDirectory() {
    if (!compiled) {
      throw new IllegalStateException(
          "You first have to compile the project. Use method compile()");
    }
    return pathToProjectRoot + File.separator + "target" + File.separator + "jimple";
  }

  private void buildJimple() throws MavenBuildException {
    Jbc2JimpleConverter converter = new Jbc2JimpleConverter();
    try {
      converter.convert(getBuildDirectory(), getJimpleDirectory());
    } catch (IOException e) {
      throw new MavenBuildException("Was not able to convert class files to Jimple", e);
    }
  }
}
