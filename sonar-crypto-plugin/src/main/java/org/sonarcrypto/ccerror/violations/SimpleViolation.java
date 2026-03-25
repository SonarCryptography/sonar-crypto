package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public record SimpleViolation(
    CryptoRulesDefinition rulesDefinition, @Nullable CallInfo callInfo, String subMessage)
    implements Violation {

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    CallInfo.createMessage(callInfo, rulesDefinition.getDefinitionKey(), messageBuilder);
    messageBuilder.append(subMessage);
  }

  public static SimpleViolation general(CallInfo callInfo) {
    return general(callInfo, null);
  }

  public static SimpleViolation general(CallInfo callInfo, @Nullable String subMessage) {
    return of(CryptoRulesDefinitions.GENERAL, callInfo, subMessage);
  }

  public static SimpleViolation of(RuleKind ruleKind, CallInfo callInfo) {
    return of(ruleKind, callInfo, null);
  }

  public static SimpleViolation of(
      RuleKind ruleKind, CallInfo callInfo, @Nullable String subMessage) {
    return of(CryptoRulesDefinitions.fromRuleKind(ruleKind), callInfo, subMessage);
  }

  public static SimpleViolation of(
      CryptoRulesDefinition rulesDefinition, CallInfo callInfo, @Nullable String subMessage) {
    return new SimpleViolation(
        rulesDefinition,
        callInfo,
        subMessage != null ? subMessage : "was cryptographically improper generated.");
  }
}
