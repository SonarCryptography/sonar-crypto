package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

@NullMarked
public class CryptoRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "crypto-java";
  public static final String REPOSITORY_NAME = "Cryptography Analysis";
  public static final String CC_RULE_NAME = "CC1";
  public static final RuleKey CC_RULE = RuleKey.of(REPOSITORY_KEY, CC_RULE_NAME);
  private static final String LANGUAGE_KEY = "java";

  @Override
  public void define(Context context) {
    NewRepository repository =
        context.createRepository(REPOSITORY_KEY, LANGUAGE_KEY).setName(REPOSITORY_NAME);

    repository
        .createRule(CC_RULE.rule())
        .setName("Cryptographic API Misuse")
        .setHtmlDescription(
            "Detects misuses of cryptographic APIs that could lead to security vulnerabilities.")
        .setStatus(RuleStatus.BETA)
        .setSeverity(Severity.MINOR)
        .setType(RuleType.VULNERABILITY);

    repository.done();
  }
}
