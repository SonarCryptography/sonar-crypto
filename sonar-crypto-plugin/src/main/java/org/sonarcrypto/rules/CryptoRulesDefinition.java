package org.sonarcrypto.rules;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

@NullMarked
public class CryptoRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "crypto-java";
  public static final String REPOSITORY_NAME = "Cryptography Analysis";
  private static final String LANGUAGE_KEY = "java";

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, LANGUAGE_KEY)
      .setName(REPOSITORY_NAME);

    // Define a rule for cryptographic errors detected by CogniCrypt
    repository.createRule("crypto-error")
      .setName("Cryptographic API Misuse")
      .setHtmlDescription("Detects misuses of cryptographic APIs that could lead to security vulnerabilities.")
      .setSeverity("CRITICAL")
      .setType(RuleType.VULNERABILITY);

    repository.done();
  }
}
