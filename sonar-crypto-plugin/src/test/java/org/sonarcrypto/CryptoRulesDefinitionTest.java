package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

class CryptoRulesDefinitionTest {

  @Test
  void define() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new CryptoRulesDefinition().define(context);

    RulesDefinition.Repository repository = context.repository("crypto-java");
    assertThat(repository).isNotNull();
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.name()).isEqualTo("Cryptography Analysis");
    assertThat(repository.rules()).hasSize(1);

    RulesDefinition.Rule rule = context.repository("crypto-java").rule("CC1");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Cryptographic API Misuse");
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.severity()).isEqualTo(Severity.MINOR);
    assertThat(rule.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(rule.htmlDescription()).isNotEmpty();
  }
}
