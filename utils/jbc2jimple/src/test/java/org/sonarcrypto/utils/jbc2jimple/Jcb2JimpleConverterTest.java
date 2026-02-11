package org.sonarcrypto.utils.jbc2jimple;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NullMarked;
import org.junit.Assert;
import org.junit.Test;

@NullMarked
public class Jcb2JimpleConverterTest {

  @SuppressWarnings("DataFlowIssue")
  private void convert() throws IOException, URISyntaxException {
    final var classPath = Path.of(this.getClass().getClassLoader().getResource("classes").toURI());

    final var outputDir = Files.createTempDirectory("jbc2jimple");

    new Jbc2JimpleConverter().convert(classPath.toString(), outputDir.toString());

    final var fileExists = Files.exists(outputDir.resolve("org.sonarcrypto.test.App.jimple"));

    FileUtils.deleteDirectory(outputDir.toFile());

    Assert.assertTrue(fileExists);
  }

  @SuppressWarnings("DataFlowIssue")
  private void convertCli() throws IOException, URISyntaxException {
    final var classPath = Path.of(this.getClass().getClassLoader().getResource("classes").toURI());

    final var outputDir = Files.createTempDirectory("jbc2jimple");

    final var args = new ArrayList<String>();

    args.add("-cp");
    args.add(classPath.toString());
    args.add("-jo");
    args.add(outputDir.toString());

    Jbc2JimpleConverter.main(args.toArray(String[]::new));

    final var fileExists = Files.exists(outputDir.resolve("org.sonarcrypto.test.App.jimple"));

    FileUtils.deleteDirectory(outputDir.toFile());

    Assert.assertTrue(fileExists);
  }

  @Test
  public void testWithoutBoomerangPreInterceptor() throws IOException, URISyntaxException {
    convert();
  }

  @Test
  public void testWithBoomerangPreInterceptor() throws IOException, URISyntaxException {
    convert();
  }

  @Test
  public void testCliWithoutBoomerangPreInterceptor() throws IOException, URISyntaxException {
    convertCli();
  }

  @Test
  public void testCliWithBoomerangPreInterceptor() throws IOException, URISyntaxException {
    convertCli();
  }
}
