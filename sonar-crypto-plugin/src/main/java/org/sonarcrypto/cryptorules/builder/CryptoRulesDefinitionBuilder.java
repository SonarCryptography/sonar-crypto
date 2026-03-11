package org.sonarcrypto.cryptorules.builder;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public interface CryptoRulesDefinitionBuilder {

  interface Rule {
    /** Sets the rule (key). */
    Name withRule(String rule);
  }

  interface Name {
    /** Sets the name. */
    Description withName(String name);
  }

  interface Description {
    /** Sets the description in HTML format. */
    Status withDescription(String html);
  }

  interface Status {
    Severity withStatus(RuleStatus ruleStatus);
  }

  interface Severity {
    /**
     * Sets the severity. See {@link org.sonar.api.rule.Severity} for valid values.
     *
     * @see org.sonar.api.rule.Severity
     */
    HowToFixSection withSeverity(String severity);
  }

  interface HowToFixSection extends Build {
    /** Sets the "how to fix" section description in HTML format. */
    AssessSection withHowToFixSection(String html);
  }

  interface AssessSection extends Build {
    /** Sets the "assess" section description in HTML format. */
    ResourcesSection withAssessSection(String html);
  }

  interface ResourcesSection extends Build {
    /** Sets the "resources" section description in HTML format. */
    Build withResourcesSection(String html);
  }

  interface Build {
    /** Builds the crypto rules definition. */
    CryptoRulesDefinition build();
  }
}
