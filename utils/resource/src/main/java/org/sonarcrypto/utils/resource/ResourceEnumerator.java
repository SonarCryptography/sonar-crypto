package org.sonarcrypto.utils.resource;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    this(1_000_000);
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
    final var resources = new LinkedHashSet<Path>();
    final var classLoader = this.getClass().getClassLoader();
    final var normalizedResourceFolder = normalizeResourcePath(resourceFolder);
    final var resourceUrls = classLoader.getResources(normalizedResourceFolder);

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

    resources.addAll(listClassPathResources(resourceFolder, fileNameEndsWith, filter));
    return List.copyOf(resources);
  }

  private List<Path> listClassPathResources(
      final Path resourceFolder, final String fileNameEndsWith, final Predicate<String> filter)
      throws IOException {
    final var resources = new LinkedHashSet<Path>();
    final var classPath = System.getProperty("java.class.path", "");
    if (classPath.isBlank()) {
      return List.of();
    }

    final var resourceFolderString = resourceFolder.toString();
    for (final var entry : classPath.split(java.io.File.pathSeparator)) {
      if (entry.isBlank()) {
        continue;
      }

      final var classPathEntry = Path.of(entry);
      if (Files.isDirectory(classPathEntry)) {
        final var resourceDirectory = classPathEntry.resolve(resourceFolderString);
        if (Files.isDirectory(resourceDirectory)) {
          resources.addAll(
              listFilesystemResources(
                  resourceDirectory.toUri(), resourceFolder, fileNameEndsWith, filter));
        }
        continue;
      }

      if (Files.isRegularFile(classPathEntry) && entry.endsWith(".jar")) {
        try (final var jarFile = new JarFile(classPathEntry.toFile())) {
          resources.addAll(listJarResources(jarFile, resourceFolder, fileNameEndsWith, filter));
        }
      }
    }

    return List.copyOf(resources);
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
    final var connection = (JarURLConnection) jarUrl.openConnection();
    connection.setUseCaches(false);

    try (final var jarFile = connection.getJarFile()) {
      return listJarResources(jarFile, baseDir, fileNameEndsWith, filter);
    }
  }

  private List<Path> listJarResources(
      final JarFile jarFile,
      final Path baseDir,
      final String fileNameEndsWith,
      final Predicate<String> filter) {
    final var resources = new ArrayList<Path>();
    final var baseDirString = normalizeDirectoryResourcePath(baseDir);
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
          || entryName.endsWith(".gitkeep")) {
        continue;
      }

      var indexOfLastSlash = entryName.lastIndexOf('/');
      indexOfLastSlash = indexOfLastSlash < 0 ? 0 : indexOfLastSlash + 1;

      final var entryNameWithoutEnding =
          entryName.substring(indexOfLastSlash, entryName.length() - fileNameEndsWith.length());
      if (!filter.test(entryNameWithoutEnding)) {
        continue;
      }

      resources.add(Path.of(entryName));
    }

    return resources;
  }

  private static String normalizeResourcePath(Path resourceFolder) {
    return resourceFolder.toString().replace('\\', '/');
  }

  private static String normalizeDirectoryResourcePath(Path resourceFolder) {
    var resourcePath = normalizeResourcePath(resourceFolder);
    if (!resourcePath.endsWith("/")) {
      resourcePath += "/";
    }
    return resourcePath;
  }
}
