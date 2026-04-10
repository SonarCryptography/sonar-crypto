package org.sonarcrypto.utils.sonar;

import boomerang.scope.WrappedClass;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

public class SonarFileSystemUtils {
  /**
   * Finds the InputFile corresponding to a WrappedClass.
   *
   * @param fileSystem the file system to search in
   * @param wrappedClass the class to find the source file for
   * @return the InputFile, or null if not found
   */
  @Nullable
  public static InputFile findInputFile(FileSystem fileSystem, WrappedClass wrappedClass) {
    String fullyQualifiedName = wrappedClass.getFullyQualifiedName();

    // Convert fully qualified class name to file path,
    // e.g., "com.example.MyClass" -> "com/example/MyClass.java"
    String relativePath = fullyQualifiedName.replace('.', '/') + ".java";

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
