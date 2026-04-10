package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public record CallViolation(CryptoRulesDefinition rulesDefinition, String subMessage)
    implements Violation {

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    messageBuilder.append(subMessage);
  }

  public static CallViolation general(@Nullable String subMessage) {
    return of(CryptoRulesDefinitions.GENERAL, subMessage);
  }

  public static CallViolation of(RuleKind ruleKind) {
    return of(ruleKind, null);
  }

  public static CallViolation of(RuleKind ruleKind, @Nullable String subMessage) {
    return of(CryptoRulesDefinitions.fromRuleKind(ruleKind), subMessage);
  }

  public static CallViolation of(
      CryptoRulesDefinition rulesDefinition, @Nullable String subMessage) {
    return new CallViolation(
        rulesDefinition,
        subMessage != null ? subMessage : "was cryptographically improper generated.");
  }
}
