package org.sonarcrypto.cryptorules;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;

@NullMarked
public interface CryptoRulesDefinitionBuilder {

  interface Rule {
    Name withRule(String rule);
  }

  interface Name {
    Description withName(String name);
  }

  interface Description {
    Status withDescription(String html);
  }

  interface Status extends Build {
    Severity withStatus(RuleStatus ruleStatus);
  }

  interface Severity extends Build {
    HowToFixSection withSeverity(String severity);
  }

  interface HowToFixSection extends Build {
    AssessSection withHowToFixSection(String html);
  }

  interface AssessSection extends Build {
    ResourcesSection withAssessSection(String html);
  }

  interface ResourcesSection extends Build {
    Build withResourcesSection(String html);
  }

  interface Build {
    CryptoRulesDefinition build();
  }
}
