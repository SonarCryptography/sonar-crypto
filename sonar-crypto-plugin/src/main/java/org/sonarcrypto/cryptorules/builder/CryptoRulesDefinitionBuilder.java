package org.sonarcrypto.cryptorules.builder;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.cryptorules.Severity;

@NullMarked
public interface CryptoRulesDefinitionBuilder {

  interface WithRuleKind {
    /** Sets the rule kind. */
    WithName withRuleKind(RuleKind ruleKind);
  }

  interface WithName {
    /** Sets the name. */
    WithKey withName(String name);
  }

  interface WithKey extends WithStatus {
    /** Sets the definition key. */
    WithStatus withKey(String definitionKey);
  }

  interface WithStatus {
    WithSeverity withStatus(RuleStatus ruleStatus);
  }

  interface WithSeverity {
    /** Sets the severity. */
    WithDescription withSeverity(Severity severity);
  }

  interface WithDescription extends WithHowToFixSection {
    /** Sets the description as well as the "why is this an issue" section in HTML format. */
    WithHowToFixSection withDescription(@Language("html") String html);
  }

  interface WithHowToFixSection extends WithResourcesSection {
    /** Sets the "how to fix" section in HTML format. */
    WithResourcesSection withHowToFixSection(@Language("html") String html);
  }

  interface WithResourcesSection extends Build {
    /** Sets the "resources" section in HTML format. */
    Build withResourcesSection(@Language("html") String html);
  }

  interface Build {
    /** Builds the crypto rules definition. */
    CryptoRulesDefinition build();
  }
}
