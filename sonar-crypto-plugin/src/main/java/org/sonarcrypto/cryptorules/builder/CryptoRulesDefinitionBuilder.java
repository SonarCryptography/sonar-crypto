package org.sonarcrypto.cryptorules.builder;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.cryptorules.Severity;

@NullMarked
public interface CryptoRulesDefinitionBuilder {

  interface WithRule {
    /** Sets the rule (key). */
    WithName withRule(String rule);
  }

  interface WithName {
    /** Sets the name. */
    WithDescription withName(String name);
  }

  interface WithDescription {
    /** Sets the description in HTML format. */
    WithStatus withDescription(@Language("html") String html);
  }

  interface WithStatus {
    WithSeverity withStatus(RuleStatus ruleStatus);
  }

  interface WithSeverity {
    /** Sets the severity. */
    WithAssessSection withSeverity(Severity severity);
  }

  interface WithAssessSection extends WithHowToFixSection {
    /** Sets the "assess" section description in HTML format. */
    WithHowToFixSection withAssessSection(@Language("html") String html);
  }

  interface WithHowToFixSection extends WithResourcesSection {
    /** Sets the "how to fix" section description in HTML format. */
    WithResourcesSection withHowToFixSection(@Language("html") String html);
  }

  interface WithResourcesSection extends Build {
    /** Sets the "resources" section description in HTML format. */
    Build withResourcesSection(@Language("html") String html);
  }

  interface Build {
    /** Builds the crypto rules definition. */
    CryptoRulesDefinition build();
  }
}
