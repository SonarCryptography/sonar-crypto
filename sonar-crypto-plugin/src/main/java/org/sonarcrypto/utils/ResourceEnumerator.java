package org.sonarcrypto.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Source: <a href="https://www.codestudy.net/blog/get-a-list-of-resources-from-classpath-directory/">How to Get a List of Resources from a Classpath Directory in Java (Works for Filesystem & JAR Files)</a>
 */
public class ResourceEnumerator {
	
	/**
	 * Lists all resources in a classpath directory (supports filesystem and JAR resources).
	 *
	 * @param directoryPath Path to the classpath directory (e.g., "configs/").
	 * @return List of resource names (relative to the classpath).
	 * @throws IOException If an I/O error occurs.
	 * @throws URISyntaxException If the resource URL is invalid.
	 */
	public static List<Path> listResources(Path directoryPath, String fileNameEndsWith) throws IOException, URISyntaxException {
		final var resources = new ArrayList<Path>();
		final var classLoader = ResourceEnumerator.class.getClassLoader();
		
		// Normalize the directory path (ensure it ends with a slash)
		//final var normalizedDir = directoryPath.endsWith("/") ? directoryPath : directoryPath + "/";
		
		// Get all URLs for the directory (handles multiple classpath entries)
		final var resourceUrls = classLoader.getResources(directoryPath.toString());
		
		while(resourceUrls.hasMoreElements()) {
			final var resourceUrl = resourceUrls.nextElement();
			final var protocol = resourceUrl.getProtocol();
			
			if("file".equals(protocol)) {
				// Handle filesystem resources
				resources.addAll(listFilesystemResources(resourceUrl.toURI(), directoryPath, fileNameEndsWith));
				//} else if ("jar".equals(protocol)) {
				//	// Handle JAR resources
				//	resources.addAll(listJarResources(resourceUrl, normalizedDir));
			}
			else {
				throw new IOException("Unsupported protocol: " + protocol);
			}
		}
		
		return resources;
	}
	
	// List resources from a filesystem directory
	private static List<Path> listFilesystemResources(URI dirUri, Path baseDir, String fileNameEndsWith) throws IOException {
		final var resources = new ArrayList<Path>();
		final var dirPath = Paths.get(dirUri);
		
		try(final var pathStream = Files.walk(dirPath)) {
			pathStream.filter(Files::isRegularFile) // Skip directories
				.filter(it -> it.getFileName().toString().endsWith(fileNameEndsWith))
				.forEach(filePath -> {
					// Get path relative to the base directory
					final var relativePath = baseDir.resolve(dirPath.relativize(filePath));
					resources.add(Path.of("/").resolve(relativePath));
				});
		}
		return resources;
	}
	
	//// List resources from a JAR entry
	//private static List<String> listJarResources(URL jarUrl, String baseDir) throws IOException {
	//	List<String> resources = new ArrayList<>();
	//	String jarUrlString = jarUrl.toString();
	//	
	//	// Parse JAR URL: format is "jar:file:/path/to/jar.jar!/{entry}"
	//	String jarPath = jarUrlString.substring(4, jarUrlString.indexOf("!"));
	//	String entryPath = jarUrlString.substring(jarUrlString.indexOf("!") + 2); // Skip "!/"
	//	
	//	// Decode URL-encoded characters (e.g., spaces as %20)
	//	jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);
	//	
	//	try(JarFile jarFile = new JarFile(jarPath)) {
	//		Enumeration<JarEntry> entries = jarFile.entries();
	//		while(entries.hasMoreElements()) {
	//			JarEntry entry = entries.nextElement();
	//			String entryName = entry.getName();
	//			
	//			// Filter entries under the base directory (and skip directories)
	//			if(entryName.startsWith(baseDir) && !entry.isDirectory()) {
	//				resources.add(entryName);
	//			}
	//		}
	//	}
	//	return resources;
	//}
}
