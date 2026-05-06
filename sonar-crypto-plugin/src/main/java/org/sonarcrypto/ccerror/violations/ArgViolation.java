package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.violations.reasons.Reason;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public final class ArgViolation extends Violation {

  private final @Nullable CallInfo callInfo;

  public ArgViolation(
      CryptoRulesDefinition rulesDefinition, @Nullable CallInfo callInfo, Reason reason) {
    super(rulesDefinition, reason);
    this.callInfo = callInfo;
  }

  public ArgViolation(RuleKind ruleKind, @Nullable CallInfo callInfo, Reason reason) {
    super(ruleKind, reason);
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
