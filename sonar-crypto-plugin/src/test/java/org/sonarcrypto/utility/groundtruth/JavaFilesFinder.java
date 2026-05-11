package org.sonarcrypto.utility.groundtruth;

import java.util.List;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

@NullMarked
public class JavaFilesFinder {
  public static List<InputFile> findJavaFiles(FileSystem fileSystem) {
    FilePredicates predicates = fileSystem.predicates();
    Iterable<InputFile> files =
        fileSystem.inputFiles(
            predicates.and(
                predicates.hasType(InputFile.Type.MAIN),
                predicates.hasLanguage("java"),
                predicates.matchesPathPattern("/**/*.java")));

    return StreamSupport.stream(files.spliterator(), false).toList();
  }
}
