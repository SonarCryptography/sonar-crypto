# sonar-crypto

SonarQube plugin that detects cryptographic API misuses in Java code using CogniCrypt (HeadlessJavaScanner from Fraunhofer IEM).

## Project structure

Multi-module Maven project (Java 17):

- `sonar-crypto-plugin/` — The SonarQube plugin (packaged as `sonar-plugin` via maven-shade-plugin)
- `e2e/` — End-to-end tests using SonarQube Orchestrator
- `utils/jbc2jimple/` — Utility for converting Java bytecode to Jimple

### Plugin classes (`org.sonarcrypto`)

- `CryptoPlugin` — Registers extensions: `CryptoRulesDefinition`, `CryptoQualityProfile`, `CryptoSensor`
- `CryptoRulesDefinition` — Defines rule repository `crypto-java` with rule `CC1` (Cryptographic API Misuse)
- `CryptoQualityProfile` — Built-in quality profile "Crypto Security" for Java (set as default)
- `CryptoSensor` — Runs CogniCrypt analysis: compiles the Maven project, extracts CrySL rules, runs HeadlessJavaScanner, reports errors
- `CcToSonarIssues` — Converts CogniCrypt errors (`Table<WrappedClass, Method, Set<AbstractError>>`) to SonarQube issues
- `analysis.CryptoAnalysisInfo` — Ordered per-run metrics keyed by `analysis.Metric<T>` (run metadata, timings, error counts, seeds, outcome); `time()`/`timeAndGet()` record durations even when the action throws; `asMap()` gives a serialization-friendly view, `writeJson()` writes it (Gson) with a schema version and timestamp; `CryptoSensor` writes it after every run (also failed ones) to `crypto-metrics.json` in the work dir or to `sonar.crypto.metricsFile`
- `analysis.Metric` — Typed metric key; each knows how to convert its value to a portable form (package-private factories)
- `analysis.MetricDefinitions` — All metric constants; defines the layout of the metrics JSON
- `analysis.Outcome` — How far a run got (`SUCCESS`, `RULE_EXTRACTION_FAILED`, `COMPILE_FAILED`, `ANALYSIS_FAILED`)

### E2E tests

- `OrchestratorTests` — Base class that starts a SonarQube instance with the sonar-java and sonar-crypto plugins
- `E2ETests` — Runs analysis on sample projects in `e2e/src/test/resources/`

## Build commands

```bash
# Build and run unit tests (excludes E2E)
mvn verify -pl '!e2e'

# Run only sonar-crypto-plugin unit tests
mvn test -pl sonar-crypto-plugin

# Run E2E tests (requires plugin to be built first)
mvn install -pl '!e2e' -DskipTests
mvn test -pl e2e

# Check code formatting
mvn spotless:check

# Apply code formatting
mvn spotless:apply
```

## Code style

- Formatting enforced by Spotless with Google Java Format; a pre-commit hook runs `mvn spotless:apply` automatically
- Null safety annotations via JSpecify (`@NullMarked`, `@Nullable`)
- Use `var` for local variable type inference where the type is obvious from context
- Keep production code simple; avoid unnecessary abstractions or refactoring just for testability

## Test conventions

- Tests use JUnit 5, AssertJ, and Mockito
- SonarQube test fixtures: `SensorContextTester`, `TestInputFileBuilder`, `LogTesterJUnit5`
- Use Mockito mocks instead of anonymous class implementations
- Inline one-liner test helpers; only extract helpers that have multiple statements
- Prefer fewer comprehensive tests over many granular ones — consolidate related assertions into a single test
- Name test methods after the method under test (e.g., `define()`, `describe()`) rather than verbose scenario descriptions
- Exception: tests for distinct behaviors of the same method get descriptive names (e.g., `find_input_file_returns_null_when_no_match`)

## CI

GitHub Actions (`.github/workflows/build.yml`):
1. Spotless check
2. Build & unit tests on Linux (with SonarCloud analysis and JaCoCo coverage)
3. Build & unit tests on Windows
4. E2E tests on Linux

## Key dependencies

- SonarQube Plugin API: `sonar-plugin-api` (provided at runtime)
- CogniCrypt: `HeadlessJavaScanner` 5.0.1 from Fraunhofer IEM
- CrySL rulesets: JCA, BouncyCastle, Tink, BouncyCastle-JCA (downloaded during build via maven-dependency-plugin)
- Shading: maven-shade-plugin with `minimizeJar=true` bundles all runtime deps into the plugin JAR

## Known quirks

- The shaded plugin JAR bundles a minimized subset of commons-io that can conflict with other classpath entries at runtime
- `AbstractError` overrides `equals`/`hashCode` based on its fields; mocked instances with null fields are all considered equal by `Set.of()`
- Plugin/CogniCrypt/ruleset versions in the metrics come from the Maven-filtered resource `org/sonarcrypto/versions.properties`; outside a Maven build they show up as unfiltered `${...}` placeholders
- E2E tests require `sonar.working.directory` set to a temp directory (CI runners may not allow writing to the source tree)
- The sonar-java plugin (8.22.0.41895) must be loaded alongside sonar-crypto in E2E to provide Java language infrastructure
