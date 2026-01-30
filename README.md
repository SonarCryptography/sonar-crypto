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

## Modules / Repository Contents

### Sonar Crypto Plugin

The plugin itself can be found in the [sonar-crypto-plugin](sonar-crypto-plugin) module.

### End-to-End (E2E) / Orchestrator Tests

These tests launch a SonarQube (SQ) instance, deploy the Sonar Crypto plugin, and run analysis on a sample project to verify the plugin's functionality.
These test can be found in the [e2e](e2e) module.
The test project can be found in the respective [resources](e2e/src/test/resources).
