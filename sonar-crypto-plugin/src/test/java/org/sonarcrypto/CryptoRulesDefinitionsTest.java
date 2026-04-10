package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

class CryptoRulesDefinitionsTest {

  @Test
  void define() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    CryptoRulesDefinitions.ALGORITHM.define(context);

    RulesDefinition.Repository repository = context.repository("crypto-java");
    assertThat(repository).isNotNull();
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.name()).isEqualTo("Cryptography Analysis");
    assertThat(repository.rules()).hasSize(1);

    RulesDefinition.Rule rule =
        context
            .repository("crypto-java")
            .rule(CryptoRulesDefinitions.ALGORITHM.getRuleKey().rule());
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Unsecure Algorithm");
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(rule.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(rule.htmlDescription()).isNotEmpty();
  }
}
