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
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.cryptorules.builder.CryptoRulesDefinitionBuilder;
import org.sonarcrypto.cryptorules.builder.CryptoRulesDefinitionBuilderImpl;

@NullMarked
public class CryptoRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "crypto-java";
  public static final String REPOSITORY_NAME = "Cryptography Analysis";
  public static final String LANGUAGE_KEY = "java";

  private final RuleKind ruleKind;
  private final RuleKey ruleKey;
  private final String name;
  private final String description;
  private final @Nullable String definitionKey;
  private final RuleStatus ruleStatus;
  private final Severity severity;
  private final @Nullable String assessSection;
  private final @Nullable String howToFixSection;
  private final @Nullable String resourceSection;

  public CryptoRulesDefinition(
      RuleKind ruleKind,
      String name,
      String description,
      @Nullable String definitionKey,
      RuleStatus ruleStatus,
      Severity severity,
      @Nullable String assessSection,
      @Nullable String howToFixSection,
      @Nullable String resourceSection) {
    this.ruleKind = ruleKind;
    this.ruleKey = RuleKey.of(REPOSITORY_KEY, "CC" + (ruleKind.ordinal() + 1));
    this.name = name;
    this.description = description;
    this.definitionKey = definitionKey;
    this.ruleStatus = ruleStatus;
    this.severity = severity;
    this.assessSection = assessSection;
    this.howToFixSection = howToFixSection;
    this.resourceSection = resourceSection;
  }

  public RuleKind getRuleKind() {
    return ruleKind;
  }

  public String getName() {
    return name;
  }

  public @Nullable String getDefinitionKey() {
    return definitionKey;
  }

  /** Gets the rule key. */
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  @Override
  public void define(Context context) {
    final var repository =
        context.createRepository(ruleKey.repository(), LANGUAGE_KEY).setName(REPOSITORY_NAME);

    final var newRule =
        repository
            .createRule(ruleKey.rule())
            .setName(name)
            .setHtmlDescription(description)
            .setStatus(ruleStatus)
            .setSeverity(severity.toRuleSeverity())
            .setType(RuleType.VULNERABILITY)
            .setCleanCodeAttribute(CleanCodeAttribute.TRUSTWORTHY)
            .addDefaultImpact(SoftwareQuality.SECURITY, severity.toImpactSeverity());

    if (howToFixSection != null) {
      newRule.addDescriptionSection(
          RuleDescriptionSection.builder()
              .sectionKey(RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY)
              .htmlContent(howToFixSection)
              .build());
    }

    if (assessSection != null) {
      newRule.addDescriptionSection(
          RuleDescriptionSection.builder()
              .sectionKey(RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY)
              .htmlContent(assessSection)
              .build());
    }

    if (resourceSection != null) {
      newRule.addDescriptionSection(
          RuleDescriptionSection.builder()
              .sectionKey(RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY)
              .htmlContent(resourceSection)
              .build());
    }

    repository.done();
  }

  /** Creates a crypto rules definition builder. */
  public static CryptoRulesDefinitionBuilder.WithRuleKind builder() {
    return new CryptoRulesDefinitionBuilderImpl();
  }
}
