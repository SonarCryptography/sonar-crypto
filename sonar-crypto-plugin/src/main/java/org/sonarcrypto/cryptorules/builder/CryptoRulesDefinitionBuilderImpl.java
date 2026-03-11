package org.sonarcrypto.cryptorules.builder;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@SuppressWarnings("NotNullFieldNotInitialized")
@NullMarked
public class CryptoRulesDefinitionBuilderImpl
    implements CryptoRulesDefinitionBuilder.Rule,
        CryptoRulesDefinitionBuilder.Name,
        CryptoRulesDefinitionBuilder.Description,
        CryptoRulesDefinitionBuilder.Status,
        CryptoRulesDefinitionBuilder.Severity,
        CryptoRulesDefinitionBuilder.HowToFixSection,
        CryptoRulesDefinitionBuilder.AssessSection,
        CryptoRulesDefinitionBuilder.ResourcesSection,
        CryptoRulesDefinitionBuilder.Build {
  public CryptoRulesDefinitionBuilderImpl() {}

  private String rule;
  private String name;
  private String description;
  private RuleStatus ruleStatus;
  private String severity;
  private @Nullable String assessSection;
  private @Nullable String howToFixSection;
  private @Nullable String resourceSection;

  @Override
  public CryptoRulesDefinitionBuilder.Name withRule(String rule) {
    this.rule = rule;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.Description withName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.Status withDescription(String html) {
    this.description = html;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.Severity withStatus(RuleStatus ruleStatus) {
    this.ruleStatus = ruleStatus;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.HowToFixSection withSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.ResourcesSection withAssessSection(String html) {
    this.assessSection = html;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.AssessSection withHowToFixSection(String html) {
    this.howToFixSection = html;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.Build withResourcesSection(String html) {
    this.resourceSection = html;
    return this;
  }

  @Override
  public CryptoRulesDefinition build() {
    return new CryptoRulesDefinition(
        this.rule,
        this.name,
        this.description,
        this.ruleStatus,
        this.severity,
        this.assessSection,
        this.howToFixSection,
        this.resourceSection);
  }
}
