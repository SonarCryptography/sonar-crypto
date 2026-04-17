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

