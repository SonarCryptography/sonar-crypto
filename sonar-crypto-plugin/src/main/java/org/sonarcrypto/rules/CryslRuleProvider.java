package org.sonarcrypto.rules;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a method to download and extract CrySL rules from the CROSSING repository. The repo
 * currently contains rules for three Crypto Libraries : BouncyCastle, BouncyCastle-JCA, and
 * JavaCryptographicArchitecture. The rulesets can not be used at the same time due to conflicting
 * file names. The rules are extracted to a temporary directory.
 */
public class CryslRuleProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(CryslRuleProvider.class);

  private static final URI RULE_DISTRIBUTION =
      URI.create("https://github.com/CROSSINGTUD/Crypto-API-Rules/archive/refs/heads/master.zip");

  private final HttpClient http = HttpClient.newHttpClient();

  /**
   * Downloads the zip from ruleDistribution, extracts all .crysl files to a temp directory, and
   * returns the directory.
   *
   * @return The temp directory containing the extracted .crysl files.
   * @throws IOException if an I/O error occurs
   */
  public Path extractCryslFilesToTempDir(Predicate<String> filter)
      throws IOException, InterruptedException {
    Path tempDir = Files.createTempDirectory("crysl_rules");
    int count = 0;

    HttpRequest req = HttpRequest.newBuilder(RULE_DISTRIBUTION).GET().build();
    HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());

    if (resp.statusCode() != 200) {
      throw new IOException("Failed to download rules: HTTP " + resp.statusCode());
    }

    try (InputStream is = resp.body();
        ZipInputStream zis = new ZipInputStream(is)) {
      ZipEntry entry;

      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory()
            && entry.getName().endsWith(".crysl")
            && filter.test(entry.getName())) {
          Path outFile = tempDir.resolve(Paths.get(entry.getName()).getFileName());
          Files.write(outFile, zis.readAllBytes());
          count++;
        }
        zis.closeEntry();
      }
    }
    LOGGER.info("Extracted {} CrySL files to {}", count, tempDir.toAbsolutePath());
    return tempDir;
  }
}
