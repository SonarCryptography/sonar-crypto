package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public final class ValueViolation extends Violation {

  private final @Nullable CallInfo callInfo;

  public ValueViolation(
      CryptoRulesDefinition rulesDefinition, @Nullable CallInfo callInfo, Cause cause) {
    super(rulesDefinition, cause);
    this.callInfo = callInfo;
  }

  public ValueViolation(RuleKind ruleKind, @Nullable CallInfo callInfo, Cause cause) {
    super(ruleKind, cause);
    this.callInfo = callInfo;
  }

  public @Nullable CallInfo getCallInfo() {
    return this.callInfo;
  }

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    final var definitionKey = getRulesDefinition().getDefinitionKey();
    final var calleeInfo = getCallInfo();

    CallInfo.createMessage(calleeInfo, definitionKey, messageBuilder);
    this.getReason().createMessage(messageBuilder);
  }
}
