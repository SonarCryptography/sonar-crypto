# Crypto Test Project

A Maven test project containing **intentional cryptographic vulnerabilities** for testing the SonarCrypto plugin.

⚠️ **WARNING: This project contains deliberately insecure code. DO NOT use any of this code in production!**

## Purpose

This project serves as a test case for the SonarCrypto plugin, containing various cryptographic vulnerabilities that should be detected by the plugin during static analysis. It helps validate that the plugin correctly identifies common crypto-related security issues.

## How to Use

### 1. Build the Project
```bash
# Navigate to the crypto-test-project directory
cd crypto-test-project

# Compile the project
mvn clean compile

# Run tests (if any)
mvn test
```

### 2. Analyze with SonarQube

#### Option A: Using SonarQube Scanner
```bash
# Run SonarQube analysis
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000
```

#### Option B: Using SonarQube Scanner CLI
```bash
# Install SonarQube Scanner CLI first, then run:
sonar-scanner \
  -Dsonar.projectKey=crypto-test-project \
  -Dsonar.sources=src/main/java \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<your-token>
```

### 3. Review Results
1. Open SonarQube web interface
2. Navigate to the `crypto-test-project` project
3. Check the **Issues** tab for crypto-related vulnerabilities
4. Verify that the SonarCrypto plugin has detected the intentional vulnerabilities