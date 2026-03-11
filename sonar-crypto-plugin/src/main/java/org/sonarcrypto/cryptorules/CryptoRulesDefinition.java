package org.sonarcrypto.cryptorules;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarcrypto.cryptorules.builder.CryptoRulesDefinitionBuilder;
import org.sonarcrypto.cryptorules.builder.CryptoRulesDefinitionBuilderImpl;

@NullMarked
public class CryptoRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "crypto-java";
  public static final String REPOSITORY_NAME = "Cryptography Analysis";
  public static final String LANGUAGE_KEY = "java";

  private final RuleKey ruleKey;
  private final String name;
  private final String description;
  private final RuleStatus ruleStatus;
  private final String severity;
  private final @Nullable String assessSection;
  private final @Nullable String howToFixSection;
  private final @Nullable String resourceSection;

  public CryptoRulesDefinition(
      String rule,
      String name,
      String description,
      RuleStatus ruleStatus,
      String severity,
      @Nullable String assessSection,
      @Nullable String howToFixSection,
      @Nullable String resourceSection) {
    this.ruleKey = RuleKey.of(REPOSITORY_KEY, rule);
    this.name = name;
    this.description = description;
    this.ruleStatus = ruleStatus;
    this.severity = severity;
    this.assessSection = assessSection;
    this.howToFixSection = howToFixSection;
    this.resourceSection = resourceSection;
  }

  /** Gets the rule key. */
  public RuleKey getRuleKey() {
    return this.ruleKey;
  }

  @Override
  public void define(Context context) {
    final var repository =
        context.createRepository(this.ruleKey.repository(), LANGUAGE_KEY).setName(REPOSITORY_NAME);

    final var newRule =
        repository
            .createRule(this.ruleKey.rule())
            .setName(this.name)
            .setHtmlDescription(this.description)
            .setStatus(this.ruleStatus)
            .setSeverity(this.severity)
            .setType(RuleType.VULNERABILITY)
            .setCleanCodeAttribute(CleanCodeAttribute.TRUSTWORTHY)
            .addDefaultImpact(
                SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER);

    if (this.howToFixSection != null) {
      newRule.addDescriptionSection(
          RuleDescriptionSection.builder()
              .sectionKey(RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY)
              .htmlContent(this.howToFixSection)
              .build());
    }

    if (this.assessSection != null) {
      newRule.addDescriptionSection(
          RuleDescriptionSection.builder()
              .sectionKey(RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY)
              .htmlContent(this.assessSection)
              .build());
    }

    if (this.resourceSection != null) {
      newRule.addDescriptionSection(
          RuleDescriptionSection.builder()
              .sectionKey(RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY)
              .htmlContent(this.resourceSection)
              .build());
    }

    repository.done();
  }

  /** Creates a crypto rules definition builder. */
  public static CryptoRulesDefinitionBuilder.Rule builder() {
    return new CryptoRulesDefinitionBuilderImpl();
  }
}
