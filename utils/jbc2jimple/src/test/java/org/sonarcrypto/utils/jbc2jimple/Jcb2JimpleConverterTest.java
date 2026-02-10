package org.sonarcrypto.utils.jbc2jimple;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;

public class Jcb2JimpleConverterTest {
  @SuppressWarnings("DataFlowIssue")
  private void convert(boolean enableBoomerangPreInterceptor)
      throws IOException, URISyntaxException {
    final var classPath = Path.of(this.getClass().getClassLoader().getResource("classes").toURI());

    final var outputDir = Files.createTempDirectory("jbc2jimple");

    new Jbc2JimpleConverter(enableBoomerangPreInterceptor)
        .convert(classPath.toString(), outputDir.toString());

    Assert.assertTrue(Files.exists(outputDir.resolve("org.sonarcrypto.test.App.jimple")));
  }

  @SuppressWarnings("DataFlowIssue")
  private void convertCli(boolean enableBoomerangPreInterceptor)
      throws IOException, URISyntaxException {
    final var classPath = Path.of(this.getClass().getClassLoader().getResource("classes").toURI());

    final var outputDir = Files.createTempDirectory("jbc2jimple");

    final var args = new ArrayList<String>();

    if (enableBoomerangPreInterceptor) args.add("-bpi");

    args.add("-cp");
    args.add(classPath.toString());
    args.add("-jo");
    args.add(outputDir.toString());

    Jbc2JimpleConverter.main(args.toArray(String[]::new));

    Assert.assertTrue(Files.exists(outputDir.resolve("org.sonarcrypto.test.App.jimple")));
  }

  @Test
  public void testWithoutBoomerangPreInterceptor() throws IOException, URISyntaxException {
    convert(false);
  }

  @Test
  public void testWithBoomerangPreInterceptor() throws IOException, URISyntaxException {
    convert(true);
  }

  @Test
  public void testCliWithoutBoomerangPreInterceptor() throws IOException, URISyntaxException {
    convertCli(false);
  }

  @Test
  public void testCliWithBoomerangPreInterceptor() throws IOException, URISyntaxException {
    convertCli(true);
  }
}
