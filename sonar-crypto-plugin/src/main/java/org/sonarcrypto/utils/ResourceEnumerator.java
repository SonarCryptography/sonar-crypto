package org.sonarcrypto.utils;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Source:
 * <a href="https://www.codestudy.net/blog/get-a-list-of-resources-from-classpath-directory/">How to Get a List of Resources from a Classpath Directory in Java (Works for Filesystem &amp; JAR Files)</a>
 */
@NullMarked
public class ResourceEnumerator {
	/**
	 * Lists all resources in a classpath directory.
	 *
	 * @param resourceFolder Path to the classpath directory.
	 * @param fileNameEndsWith File name ending to filter.
	 * @param filter A custom filter. Gets the file name without the value of
	 *               {@code fileNameEndsWith}.
	 * @return List of matched resource files.
	 * @throws IOException An I/O error occurred.
	 */
	public static List<Path> listResources(
		final Path resourceFolder,
		final String fileNameEndsWith,
		final Predicate<String> filter
	) throws IOException {
		final var resources = new ArrayList<Path>();
		final var classLoader = ResourceEnumerator.class.getClassLoader();
		final var resourceUrls = classLoader.getResources(resourceFolder.toString());
		
		while(resourceUrls.hasMoreElements()) {
			final var resourceUrl = resourceUrls.nextElement();
			final var protocol = resourceUrl.getProtocol();
			
			if("file".equals(protocol)) {
				try {
					resources.addAll(
						listFilesystemResources(
							resourceUrl.toURI(),
							resourceFolder,
							fileNameEndsWith,
							filter
						)
					);
				}
				catch(URISyntaxException e) {
					throw new IOException(
						"Failed converting resource URL into URI: Invalid URI syntax!",
						e
					);
				}
			}
			else {
				throw new IOException("Unsupported protocol: " + protocol);
			}
		}
		
		return resources;
	}
	
	private static List<Path> listFilesystemResources(
		final URI dirUri,
		final Path baseDir,
		final String fileNameEndsWith,
		final Predicate<String> filter
	) throws IOException {
		final var resources = new ArrayList<Path>();
		final var dirPath = Paths.get(dirUri);
		
		try(final var pathStream = Files.walk(dirPath)) {
			pathStream.filter(Files::isRegularFile) // Skip directories
				.filter(it -> {
                    final var fileName = it.getFileName().toString();

                    // Check the suffix first
                    if (!fileName.endsWith(fileNameEndsWith)) {
                        return false;
                    }

                    final var fileNameWithoutEnding =
                            fileName.substring(0, fileName.length() - fileNameEndsWith.length());

                    return filter.test(fileNameWithoutEnding);
				})
				.forEach(filePath -> {
					final var relativePath = baseDir.resolve(dirPath.relativize(filePath));
					resources.add(Path.of("/").resolve(relativePath));
				});
		}
		return resources;
	}
}
