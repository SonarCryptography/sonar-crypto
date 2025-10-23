/********************************************************************************
 * Copyright (c) 2017 Fraunhofer IEM, Paderborn, Germany
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.sonarcrypto.cognicrypt;

import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.commons.io.IOUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenProject {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenProject.class);
  private final String pathToProjectRoot;
  private boolean compiled;
  private String fullProjectClassPath;

  public MavenProject(String pathToProjectRoot) throws FileNotFoundException {
    File file = new File(pathToProjectRoot);
    if (!file.exists())
      throw new FileNotFoundException("The path " + pathToProjectRoot + " does not exist!");
    this.pathToProjectRoot = new File(pathToProjectRoot).getAbsolutePath();
  }

  public void compile() throws MavenBuildException {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(new File(pathToProjectRoot + File.separator + "pom.xml"));
    ArrayList<String> goals = Lists.newArrayList();
    goals.add("clean");
    goals.add("compile");
    request.setGoals(goals);

    Invoker invoker = new DefaultInvoker();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream out = new PrintStream(baos)) {
      request.setOutputHandler(new PrintStreamHandler(out, true));
      InvocationResult res = invoker.execute(request);
      if (res.getExitCode() != 0) {
        throw new MavenBuildException("Was not able to compile project " + pathToProjectRoot + ".");
      }
    } catch (MavenInvocationException | IOException e) {
      throw new MavenBuildException(
        "Was not able to invoke maven in path " + pathToProjectRoot + ". Does a pom.xml exist?",
        e);
    }
    compiled = true;
    computeClassPath();
  }

  private void computeClassPath() throws MavenBuildException {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(new File(pathToProjectRoot + File.separator + "pom.xml"));
    ArrayList<String> goals = Lists.newArrayList();
    goals.add("dependency:build-classpath");
    goals.add("-Dmdep.outputFile=\"classPath.temp\"");
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream out = new PrintStream(baos)) {
      request.setOutputHandler(new PrintStreamHandler(out, true));
      request.setGoals(goals);
      Invoker invoker = new DefaultInvoker();
      InvocationResult res = invoker.execute(request);
      if (res.getExitCode() != 0) {
        throw new MavenBuildException(
          "Was not able to compute dependencies " + pathToProjectRoot + ".");
      }
    } catch (MavenInvocationException | IOException e) {
      throw new MavenBuildException("Was not able to invoke maven to compute dependencies", e);
    }
    try {
      File classPathFile = new File(pathToProjectRoot + File.separator + "classPath.temp");
      fullProjectClassPath = IOUtils.toString(new FileInputStream(classPathFile), StandardCharsets.UTF_8);
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

  public String getFullClassPath() {
    if (!compiled) {
      throw new IllegalStateException("Project has not been compiled yet.");
    }
    return fullProjectClassPath;
  }
}
