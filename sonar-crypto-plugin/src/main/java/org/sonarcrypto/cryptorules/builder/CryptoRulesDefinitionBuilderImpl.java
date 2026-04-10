package org.sonarcrypto.cryptorules.builder;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.cryptorules.Severity;

@NullMarked
public class CryptoRulesDefinitionBuilderImpl
    implements CryptoRulesDefinitionBuilder.WithKey,
        CryptoRulesDefinitionBuilder.WithRuleKind,
        CryptoRulesDefinitionBuilder.WithName,
        CryptoRulesDefinitionBuilder.WithDescription,
        CryptoRulesDefinitionBuilder.WithStatus,
        CryptoRulesDefinitionBuilder.WithSeverity,
        CryptoRulesDefinitionBuilder.WithHowToFixSection,
        CryptoRulesDefinitionBuilder.WithAssessSection,
        CryptoRulesDefinitionBuilder.WithResourcesSection,
        CryptoRulesDefinitionBuilder.Build {
  public CryptoRulesDefinitionBuilderImpl() {}

  private @Nullable RuleKind ruleKind;
  private @Nullable String name;
  private @Nullable String description;
  private @Nullable String definitionKey;
  private @Nullable RuleStatus ruleStatus;
  private @Nullable Severity severity;
  private @Nullable String assessSection;
  private @Nullable String howToFixSection;
  private @Nullable String resourceSection;

  @Override
  public CryptoRulesDefinitionBuilder.WithName withRuleKind(RuleKind ruleKind) {
    this.ruleKind = ruleKind;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.WithDescription withName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.WithKey withDescription(String html) {
    this.description = html;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.WithStatus withKey(String definitionKey) {
    this.definitionKey = definitionKey;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.WithSeverity withStatus(RuleStatus ruleStatus) {
    this.ruleStatus = ruleStatus;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.WithAssessSection withSeverity(Severity severity) {
    this.severity = severity;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.WithHowToFixSection withAssessSection(String html) {
    this.assessSection = html;
    return this;
  }

  @Override
  public CryptoRulesDefinitionBuilder.WithResourcesSection withHowToFixSection(String html) {
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
        requireNonNull(this.ruleKind),
        requireNonNull(this.name),
        requireNonNull(this.description),
        this.definitionKey,
        requireNonNull(this.ruleStatus),
        requireNonNull(this.severity),
        this.assessSection,
        this.howToFixSection,
        this.resourceSection);
  }
}
