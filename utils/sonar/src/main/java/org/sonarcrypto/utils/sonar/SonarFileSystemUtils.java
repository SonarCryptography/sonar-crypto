package org.sonarcrypto.utils.sonar;

import java.util.Iterator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

@NullMarked
public class SonarFileSystemUtils {
  private SonarFileSystemUtils() {
    // Utility class
  }

  /**
   * Finds the InputFile corresponding to a WrappedClass.
   *
   * @param fileSystem the file system to search in
   * @param className the class to find the source file for
   * @return the InputFile, or null if not found
   */
  @Nullable
  public static InputFile findInputFile(FileSystem fileSystem, FqClassName className) {

    // Convert fully qualified class name to file path,
    // e.g., "com.example.MyClass" -> "com/example/MyClass.java"
    String relativePath = className.fqn().replace('.', '/') + ".java";

    FilePredicates predicates = fileSystem.predicates();
    Iterator<InputFile> files =
        fileSystem
            .inputFiles(
                predicates.and(
                    predicates.hasType(InputFile.Type.MAIN),
                    predicates.hasLanguage("java"),
                    predicates.matchesPathPattern("**/" + relativePath)))
            .iterator();

    return files.hasNext() ? files.next() : null;
  }
}
