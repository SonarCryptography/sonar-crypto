package org.sonarcrypto.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Source: <a
 * href="https://www.codestudy.net/blog/get-a-list-of-resources-from-classpath-directory/">How to
 * Get a List of Resources from a Classpath Directory in Java (Works for Filesystem &amp; JAR
 * Files)</a>
 */
@NullMarked
public class ResourceEnumerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceEnumerator.class);

  private final int jarEntitiesThreshold;

  /** Creates a new instance with a default JAR entity threshold of {@code 1,000} entities. */
  public ResourceEnumerator() {
    this(1000);
  }

  /**
   * Creates a new instance.
   *
   * @param jarEntitiesThreshold The entities threshold for JAR files.
   */
  public ResourceEnumerator(int jarEntitiesThreshold) {
    if (jarEntitiesThreshold < 1) throw new IllegalArgumentException("Invalid entities threshold.");

    this.jarEntitiesThreshold = jarEntitiesThreshold;
  }

  /**
   * Lists all resources in a classpath directory.
   *
   * @param resourceFolder Path to the classpath directory.
   * @param fileNameEndsWith File name ending to filter.
   * @param filter A custom filter. Gets the file name without the value of {@code
   *     fileNameEndsWith}.
   * @return List of matched resource files.
   * @throws IOException An I/O error occurred.
   * @throws URISyntaxException Should never occur, because the URI should always be well-defined.
   */
  public List<Path> listResources(
      final Path resourceFolder, final String fileNameEndsWith, final Predicate<String> filter)
      throws IOException, URISyntaxException {
    final var resources = new ArrayList<Path>();
    final var classLoader = this.getClass().getClassLoader();
    final var resourceUrls = classLoader.getResources(resourceFolder.toString());

    while (resourceUrls.hasMoreElements()) {
      final var resourceUrl = resourceUrls.nextElement();
      final var protocol = resourceUrl.getProtocol();

      if ("file".equals(protocol)) {
        resources.addAll(
            listFilesystemResources(resourceUrl.toURI(), resourceFolder, fileNameEndsWith, filter));
      } else if ("jar".equals(protocol)) {
        resources.addAll(listJarResources(resourceUrl, resourceFolder, fileNameEndsWith, filter));
      } else {
        throw new IOException("Unsupported protocol: " + protocol);
      }
    }

    return resources;
  }

  private List<Path> listFilesystemResources(
      final URI dirUri,
      final Path baseDir,
      final String fileNameEndsWith,
      final Predicate<String> filter)
      throws IOException {
    final var resources = new ArrayList<Path>();
    final var dirPath = Paths.get(dirUri);

    try (final var pathStream = Files.walk(dirPath)) {
      pathStream
          .filter(Files::isRegularFile) // Skip directories
          .filter(
              it -> {
                final var fileName = it.getFileName().toString();

                // Check the suffix first
                if (!fileName.endsWith(fileNameEndsWith) || fileName.endsWith(".gitkeep"))
                  return false;

                final var fileNameWithoutEnding =
                    fileName.substring(0, fileName.length() - fileNameEndsWith.length());

                return filter.test(fileNameWithoutEnding);
              })
          .forEach(
              filePath -> {
                final var relativePath = baseDir.resolve(dirPath.relativize(filePath));
                resources.add(relativePath);
              });
    }
    return resources;
  }

  // This function is package private for test coverage
  List<Path> listJarResources(
      final URL jarUrl,
      final Path baseDir,
      final String fileNameEndsWith,
      final Predicate<String> filter)
      throws IOException {
    final var resources = new ArrayList<Path>();
    final var jarUrlString = jarUrl.getPath();

    var baseDirString = baseDir.toString();

    // Ensure that base dir ends with a slash
    if (!baseDirString.endsWith("/")) baseDirString += "/";

    // Parse JAR URL: format is "jar:file:/path/to/jar.jar!/{entry}"
    var jarPath = jarUrlString.substring(5, jarUrlString.indexOf("!"));

    // Decode URL-encoded characters (e.g., spaces as %20)
    jarPath = URLDecoder.decode(jarPath, UTF_8);

    try (final var jarFile = new JarFile(jarPath)) {
      final var entries = jarFile.entries();
      var processedEntries = 0;

      while (entries.hasMoreElements()) {
        if (++processedEntries > jarEntitiesThreshold) {
          LOGGER.error(
              "Too many entries in JAR file: Stopped after {} entries!", jarEntitiesThreshold);

          break;
        }

        final var entry = entries.nextElement();
        final var entryName = entry.getName();

        if (!entryName.startsWith(baseDirString)
            || entry.isDirectory()
            || !entryName.endsWith(fileNameEndsWith)
            || entryName.endsWith(".gitkeep")) continue;

        var indexOfLastSlash = entryName.lastIndexOf('/');
        indexOfLastSlash = indexOfLastSlash < 0 ? 0 : indexOfLastSlash + 1;

        final var entryNameWithoutEnding =
            entryName.substring(indexOfLastSlash, entryName.length() - fileNameEndsWith.length());

        // Filter entries under the base directory (and skip directories)
        if (!filter.test(entryNameWithoutEnding)) continue;

        resources.add(Path.of(entryName));
      }
    }

    return resources;
  }
}
