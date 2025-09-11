package org.sonarcrypto.rules;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a method to extract CrySL rules from bundled resources. The rules are packaged
 * as embedded resources during build time from the CROSSING repository. The repo
 * currently contains rules for three Crypto Libraries : BouncyCastle, BouncyCastle-JCA, and
 * JavaCryptographicArchitecture. The rulesets can not be used at the same time due to conflicting
 * file names. The rules are extracted to a temporary directory.
 */
public class CryslRuleProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(CryslRuleProvider.class);

  private static final String CRYSL_RESOURCES_PATH = "/crysl-rules/";

  /**
   * Extracts .crysl files from bundled resources to a temp directory, applying the given filter.
   *
   * @param filter Predicate to filter rule files by their original path names
   * @return The temp directory containing the extracted .crysl files.
   * @throws IOException if an I/O error occurs
   */
  public Path extractCryslFilesToTempDir(Predicate<String> filter)
      throws IOException, InterruptedException {
    Path tempDir = Files.createTempDirectory("crysl_rules");
    int count = 0;

    // Get the resource URL for the crysl-rules directory
    URL resourceUrl = getClass().getResource(CRYSL_RESOURCES_PATH);
    if (resourceUrl == null) {
      throw new IOException("CrySL rules not found in resources. Make sure the build process downloaded and packaged them.");
    }

    try {
      // List all .crysl files in the resources directory
      // Since we flattened all files during build, we need to read them directly
      String[] ruleFiles = getRuleFileNames();
      
      for (String fileName : ruleFiles) {
        if (fileName.endsWith(".crysl")) {
          // Create a simulated path for filtering (based on original structure)
          String simulatedPath = getSimulatedPathForFile(fileName);
          
          if (filter.test(simulatedPath)) {
            try (InputStream is = getClass().getResourceAsStream(CRYSL_RESOURCES_PATH + fileName)) {
              if (is != null) {
                Path outFile = tempDir.resolve(fileName);
                Files.write(outFile, is.readAllBytes());
                count++;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      throw new IOException("Failed to extract CrySL rules from resources", e);
    }

    LOGGER.info("Extracted {} CrySL files to {}", count, tempDir.toAbsolutePath());
    return tempDir;
  }

  /**
   * Gets the list of rule file names from the resources.
   * This implementation dynamically discovers all .crysl files in the resources directory.
   */
  private String[] getRuleFileNames() throws IOException {
    List<String> ruleFiles = new ArrayList<>();
    URL resourceUrl = getClass().getResource(CRYSL_RESOURCES_PATH);
    
    if (resourceUrl == null) {
      return new String[0];
    }

    try {
      if ("jar".equals(resourceUrl.getProtocol())) {
        // Running from JAR - need to list entries from JAR file
        String jarPath = resourceUrl.getPath().substring(5, resourceUrl.getPath().indexOf("!"));
        try (JarFile jar = new JarFile(jarPath)) {
          Enumeration<JarEntry> entries = jar.entries();
          while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("crysl-rules/") && name.endsWith(".crysl") && !entry.isDirectory()) {
              String fileName = name.substring(name.lastIndexOf('/') + 1);
              ruleFiles.add(fileName);
            }
          }
        }
      } else {
        // Running from file system (development mode)
        try {
          Path resourcePath = Paths.get(resourceUrl.toURI());
          Files.walk(resourcePath)
               .filter(path -> path.toString().endsWith(".crysl"))
               .forEach(path -> ruleFiles.add(path.getFileName().toString()));
        } catch (URISyntaxException e) {
          throw new IOException("Failed to convert resource URL to URI", e);
        }
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to dynamically discover rule files, falling back to empty list", e);
    }

    LOGGER.debug("Discovered {} rule files in resources", ruleFiles.size());
    return ruleFiles.toArray(new String[0]);
  }

  /**
   * Creates a simulated path based on the file name to support the existing filter logic.
   * This maps flattened file names back to their likely original directory structure.
   */
  private String getSimulatedPathForFile(String fileName) {
    // Map common files to their likely source directories based on naming patterns
    if (fileName.toLowerCase().contains("bouncy") || 
        fileName.matches(".*(?i)(aes|des|rsa|dsa|ecdsa|sha|md5).*\\.crysl")) {
      return "BouncyCastle/" + fileName;
    } else if (fileName.toLowerCase().contains("jca") || 
               fileName.matches(".*(?i)(cipher|keygenerator|messagedigest|signature).*\\.crysl")) {
      return "JavaCryptographicArchitecture/" + fileName;
    } else {
      // Default to BouncyCastle for unknown files
      return "BouncyCastle/" + fileName;
    }
  }

}