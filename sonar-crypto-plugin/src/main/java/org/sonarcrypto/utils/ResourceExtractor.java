package org.sonarcrypto.utils;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@NullMarked
public class ResourceExtractor {
	/**
	 * Extracts files from resources into a target directory.
	 * 
	 * @param resourceFolder The resource folder.
	 * @param targetFolder The target folder.
	 * @param fileEnding The file ending (including the dot).
	 * @param filter The filter. Gets the file name without the value of {@code fileEnding}.
	 * @return The paths of the extracted files.
	 * @throws IOException An I/O error occurred.
	 */
	public static List<Path> extract(
		final String resourceFolder,
		final Path targetFolder,
		final String fileEnding,
		final Predicate<String> filter
	) throws IOException {
		final var collectedTargetPaths = new ArrayList<Path>();
		final var resourcePaths =
			ResourceEnumerator.listResources(Path.of(resourceFolder), fileEnding, filter);
		
		for(final var resourcePath : resourcePaths) {
			try(var stream = ResourceExtractor.class.getResourceAsStream(resourcePath.toString())) {
				if(stream == null) {
					throw new IOException(
						"Failed extracting resource: The resource stream is null!"
					);
				}
				
				Path targetPath = targetFolder.resolve(resourcePath.getFileName());
				Files.copy(stream, targetPath, REPLACE_EXISTING);
				collectedTargetPaths.add(targetPath);
			}
		}
		
		return collectedTargetPaths;
	}
}
