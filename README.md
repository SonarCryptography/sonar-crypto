# sonar-crypto

The Sonar Crypto plugin is an open-source extension for SonarQube that provides advanced cryptographic analysis capabilities originally implemented in CogniCrypt (CC).

## Prerequisites

- **Required:**
    - Maven
    - Java 17 or higher
- **Optional:**
    - For E2E tests (to use Maven Central instead of default Sonar internal mirror)
        - Set environment variable: `ARTIFACTORY_URL=https://repo1.maven.org/maven2`
        - **OR**
        - Setup `~/.sonar/orchestrator/orchestrator.properties` with this [content](doc/orchestrator.properties)

## Building & Testing

This project uses Maven as its build system.
To build the project, run the following command in the root directory:

```bash
mvn clean install -pl '!e2e'
```

This command will compile the source code, run unit tests, and package the project. It will skip the E2E tests.

> [!TIP]
> Add `-DskipTests` to skip unit tests.
>

## Code Style

This project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format to ensure consistent code formatting across the codebase.

To check if your code follows the formatting standards:

```bash
mvn spotless:check
```

To automatically format your code:

```bash
mvn spotless:apply
```

> [!IMPORTANT]
> Code formatting is enforced in the CI pipeline.

## Analysis Information

For every analysis run, `CryptoSensor` collects typed metrics into a `CryptoAnalysisInfo`
(package `org.sonarcrypto.analysis`). Each metric is a typed `Metric<T>` key defined in `MetricDefinitions`, so values are read and
written type-safely via `info.put(MetricDefinitions.X, value)` / `info.get(MetricDefinitions.X)`. `CryptoAnalysisInfo.asMap()`
returns all metrics in insertion order, keyed by name, with values converted to strings, numbers, booleans and
flat maps of those, so it can be serialized directly (e.g. to JSON):

| Metric | Description |
| --- | --- |
| `projectKey` | Key of the analyzed SonarQube project |
| `pluginVersion` | Version of the sonar-crypto plugin |
| `cognicryptVersion` | Version of CogniCrypt (CryptoAnalysis) |
| `ruleset` / `rulesetVersion` | CrySL ruleset used for the analysis and its version |
| `ruleExtractionMillis` | Time spent extracting the CrySL rules |
| `inputSource` | Whether CogniCrypt analyzed Jimple bridge output (`JIMPLE`) or a compiled Maven project (`MAVEN`) |
| `compileMillis` | Time spent compiling (Maven compile / classpath resolution) |
| `classpathFallback` | `true` if the Maven classpath could not be resolved for Jimple input and only the ruleset dependencies were used |
| `analysisMillis` | Time spent running the CogniCrypt scan itself |
| `totalErrors` | Total number of cryptographic errors found |
| `errorsPerRuleKind` | Error count per `RuleKind`; kinds without errors are listed with 0 |
| `errorsPerCryslRule` | Error count per CrySL rule (fully qualified class name of the rule's type), sorted by name |
| `seeds` | Number of crypto usage sites (seeds) CogniCrypt discovered |
| `classesWithCryptoUsage` | Number of classes containing at least one seed |
| `methodsWithCryptoUsage` | Number of methods containing at least one seed |
| `outcome` | `SUCCESS`, `RULE_EXTRACTION_FAILED`, `COMPILE_FAILED` or `ANALYSIS_FAILED` |
| `totalMillis` | Time spent in the whole sensor run |

Metrics are written for failed runs too; they then contain everything collected up to the failure
(e.g. no `analysisMillis` when compilation failed). Durations of failed steps are still recorded.

To add a metric, declare a constant in `MetricDefinitions` using one of the `Metric` factories
(`ofNumber`, `ofBoolean`, `ofString`, `ofEnum`, `ofEnumCounts`, `ofCounts`; add a new factory for
other value kinds) and call `info.put(MetricDefinitions.X, value)` in `CryptoSensor`. Durations are
best recorded with `info.time(metric, action)` / `info.timeAndGet(metric, action)`.

The metrics are logged at `INFO` level and written as JSON to `crypto-metrics.json` in the scanner
working directory (`.scannerwork/` by default). Set `sonar.crypto.metricsFile` to write them
elsewhere; relative paths are resolved against the project base directory. Failing to write the file
only logs a warning. The file looks like this:

```json
{
  "schemaVersion": 2,
  "timestamp": "2026-10-09T11:42:07.123Z",
  "metrics": {
    "projectKey": "my-project",
    "pluginVersion": "1.0-SNAPSHOT",
    "cognicryptVersion": "5.0.2-SNAPSHOT",
    "ruleset": "JCA_BC_JCA",
    "rulesetVersion": "1.0.0",
    "ruleExtractionMillis": 310,
    "inputSource": "MAVEN",
    "compileMillis": 5321,
    "classpathFallback": false,
    "analysisMillis": 8120,
    "totalErrors": 4,
    "errorsPerRuleKind": { "GENERAL": 0, "ALGORITHM": 3, "MODE": 0, "PADDING": 0, "KEY_MATERIAL": 1, "FORBIDDEN_METHOD": 0, "UNCAUGHT_EXCEPTION": 0, "API_MISUSE": 0 },
    "errorsPerCryslRule": { "java.security.MessageDigest": 3, "javax.crypto.KeyGenerator": 1 },
    "seeds": 7,
    "classesWithCryptoUsage": 2,
    "methodsWithCryptoUsage": 3,
    "outcome": "SUCCESS",
    "totalMillis": 13790
  }
}
```

`schemaVersion` is bumped whenever the layout changes incompatibly.

## Modules / Repository Contents

### Sonar Crypto Plugin

The plugin itself can be found in the [sonar-crypto-plugin](sonar-crypto-plugin) module.

### End-to-End (E2E) / Orchestrator Tests

These tests launch a SonarQube (SQ) instance, deploy the Sonar Crypto plugin, and run analysis on a sample project to verify the plugin's functionality.
These test can be found in the [e2e](e2e) module.
The test project can be found in the respective [resources](e2e/src/test/resources).

### Utility Modules

#### [utils/cognicrypt](utils/cognicrypt)
Integrates CogniCrypt/CryptoAnalysis into the plugin. Provides the `JimpleConvertingView` which loads Jimple files and applies line-number mappings back to original Java source positions, the `LocationReplacerInterceptor` for rewriting statement positions in method bodies, and the CrySL ruleset and scanner setup.

#### [utils/jbc2jimple](utils/jbc2jimple)
Converts Java bytecode to Jimple using SootUp and writes the resulting `.jimple` files and their `.map.json` sidecar files to disk. Also serves as a standalone CLI tool (`Jbc2JimpleConverter`).

#### [utils/jimple-printer](utils/jimple-printer)
**LGPL-licensed** Jimple printer derived from SootUp. Serialises SootUp's IR to `.jimple` text files while feeding position information to the `LineNumberMapper`.

#### [utils/jimple-mapper](utils/jimple-mapper)
Collects and serialises line-number mappings between generated Jimple code and original Java source positions. Produces `LineMappingCollection` objects that are written as `.map.json` sidecar files alongside each `.jimple` file.

#### [utils/maven](utils/maven)
Wraps Maven project compilation. `MavenProject` compiles a given Maven project and exposes its build output directory, Jimple output directory, and full classpath for use by the converter and test runners.

#### [utils/resource](utils/resource)
Provides utilities for extracting and enumerating classpath resources (`ResourceExtractor`, `ResourceEnumerator`). Used by the plugin to unpack bundled CrySL ruleset zips at runtime.

#### [utils/test](utils/test)
Shared test infrastructure. Provides `TestRunner` implementations (`MavenProjectTestRunner`, `JimpleTestRunner`, `ClassPathTestRunner`) and AssertJ-based assertions (`CcErrorsAssert`) for writing CryptoAnalysis integration tests.

