package org.sonarcrypto;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.cognicrypt.MavenBuildException;
import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;

@NullMarked
public class CryptoSensor implements Sensor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSensor.class);

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.name("CogniCryptSensor");
        sensorDescriptor.onlyOnLanguages("java");
    }

    @Override
    public void execute(SensorContext sensorContext) {
        FileSystem fileSystem = sensorContext.fileSystem();

        String mavenProjectPath = fileSystem.baseDir().getAbsolutePath();
        MavenProject mi;
        try {
            mi = new MavenProject(mavenProjectPath);
            mi.compile();
        } catch (IOException | MavenBuildException e) {
            LOGGER.error("Failed to build Maven project", e);
            return;
        }

        final String ruleset = "bc";
        Path ruleDir;
        try {
            CryslRuleProvider ruleProvider = new CryslRuleProvider();
            ruleDir = ruleProvider.extractRulesetToTempDir(ruleset);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(
                "I/O error extracting CrySL rules for ruleset '{}': {}", ruleset, e.getMessage(), e);
            return;
        }

        HeadlessJavaScanner scanner =
            new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());

        scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
        scanner.scan();
        var errors = scanner.getCollectedErrors();
        LOGGER.info("Found {} cryptographic errors", errors.size());

        reportAllIssues(sensorContext, errors);
    }

    private void reportAllIssues(SensorContext context, Table<WrappedClass, Method, Set<AbstractError>> issuesFromCC) {
        FileSystem fileSystem = context.fileSystem();

        for (Table.Cell<WrappedClass, Method, Set<AbstractError>> cell : issuesFromCC.cellSet()) {
            WrappedClass wrappedClass = cell.getRowKey();
            Method method = cell.getColumnKey();
            Set<AbstractError> errors = cell.getValue();

            // Find the InputFile corresponding to this class
            InputFile inputFile = findInputFile(fileSystem, wrappedClass);
            if (inputFile == null) {
                LOGGER.warn("Could not find source file for class: {}", wrappedClass.getFullyQualifiedName());
                continue;
            }

            // Report each error in this class/method
            for (AbstractError error : errors) {
                // TODO: Extract actual line number from error once API is confirmed
                // The AbstractError may have methods like getErrorLocation(), getStatement(), etc.
                // For now, report on line 1 as a placeholder
                int lineNumber = 1;
                String errorMessage = String.format("Cryptographic error in method %s: %s",
                                                    method.getName(),
                                                    error.getClass().getSimpleName());
                reportIssue(context, inputFile, lineNumber, errorMessage);
            }
        }
    }

    @Nullable
    private InputFile findInputFile(FileSystem fileSystem, WrappedClass wrappedClass) {
        String fullyQualifiedName = wrappedClass.getFullyQualifiedName();

        // Convert fully qualified class name to file path
        // e.g., "com.example.MyClass" -> "com/example/MyClass.java"
        String relativePath = fullyQualifiedName.replace('.', '/') + ".java";

        FilePredicates predicates = fileSystem.predicates();
        Iterable<InputFile> files = fileSystem.inputFiles(
            predicates.and(
                predicates.hasType(InputFile.Type.MAIN),
                predicates.hasLanguage("java"),
                predicates.matchesPathPattern("**/" + relativePath)
            )
        );

        // Return the first matching file (there should only be one)
        for (InputFile file : files) {
            return file;
        }

        return null;
    }

    private void reportIssue(SensorContext context, InputFile inputFile, int line, String errorMessage) {
        NewIssue issue = context.newIssue().forRule(CryptoRulesDefinition.CC_RULE);
        NewIssueLocation location = issue.newLocation()
                                         .on(inputFile)
                                         .at(inputFile.selectLine(line))
                                         .message("Cryptographic API misuse: " + errorMessage);
        issue.at(location).save();

        LOGGER.debug("Reported issue in {} at line {}: {}", inputFile.filename(), line, errorMessage);
    }
}
