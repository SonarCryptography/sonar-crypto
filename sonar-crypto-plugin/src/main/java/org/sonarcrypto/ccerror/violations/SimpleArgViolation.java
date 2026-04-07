package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public record SimpleArgViolation(
    CryptoRulesDefinition rulesDefinition, @Nullable CallInfo callInfo, String subMessage)
    implements Violation {

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    CallInfo.createMessage(callInfo, rulesDefinition.getDefinitionKey(), messageBuilder);
    messageBuilder.append(subMessage);
  }

  public static SimpleArgViolation general(CallInfo callInfo) {
    return general(callInfo, null);
  }

  public static SimpleArgViolation general(CallInfo callInfo, @Nullable String subMessage) {
    return of(CryptoRulesDefinitions.GENERAL, callInfo, subMessage);
  }

  public static SimpleArgViolation of(RuleKind ruleKind, CallInfo callInfo) {
    return of(ruleKind, callInfo, null);
  }

  public static SimpleArgViolation of(
      RuleKind ruleKind, CallInfo callInfo, @Nullable String subMessage) {
    return of(CryptoRulesDefinitions.fromRuleKind(ruleKind), callInfo, subMessage);
  }

  public static SimpleArgViolation of(
      CryptoRulesDefinition rulesDefinition, CallInfo callInfo, @Nullable String subMessage) {
    return new SimpleArgViolation(
        rulesDefinition,
        callInfo,
        subMessage != null ? subMessage : "was cryptographically improper generated.");
  }
}
