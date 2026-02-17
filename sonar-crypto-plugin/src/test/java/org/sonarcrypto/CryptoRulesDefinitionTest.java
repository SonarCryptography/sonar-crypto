package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

class CryptoRulesDefinitionTest {

  @Test
  void define_creates_repository_with_correct_key_and_language() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new CryptoRulesDefinition().define(context);

    RulesDefinition.Repository repository = context.repository("crypto-java");
    assertThat(repository).isNotNull();
    assertThat(repository.language()).isEqualTo("java");
  }

  @Test
  void define_creates_repository_with_correct_name() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new CryptoRulesDefinition().define(context);

    RulesDefinition.Repository repository = context.repository("crypto-java");
    assertThat(repository.name()).isEqualTo("Cryptography Analysis");
  }

  @Test
  void define_creates_single_rule() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new CryptoRulesDefinition().define(context);

    RulesDefinition.Repository repository = context.repository("crypto-java");
    assertThat(repository.rules()).hasSize(1);
  }

  @Test
  void define_cc1_rule_has_correct_properties() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new CryptoRulesDefinition().define(context);

    RulesDefinition.Rule rule = context.repository("crypto-java").rule("CC1");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Cryptographic API Misuse");
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.severity()).isEqualTo(Severity.MINOR);
    assertThat(rule.type()).isEqualTo(RuleType.VULNERABILITY);
  }

  @Test
  void define_cc1_rule_has_html_description() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new CryptoRulesDefinition().define(context);

    RulesDefinition.Rule rule = context.repository("crypto-java").rule("CC1");
    assertThat(rule.htmlDescription()).isNotEmpty();
  }
}
