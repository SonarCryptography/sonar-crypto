package org.sonarcrypto.ccerror.violations;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.FlowEntry;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class ValueViolation extends Violation {

  private final @Nullable CallInfo callInfo;

  public ValueViolation(
      CryptoRulesDefinition rulesDefinition,
      @Nullable CallInfo callInfo,
      Cause cause,
      List<FlowEntry> flow) {
    super(rulesDefinition, cause, flow);
    this.callInfo = callInfo;
  }

  public ValueViolation(
      RuleKind ruleKind, @Nullable CallInfo callInfo, Cause cause, List<FlowEntry> flow) {
    super(ruleKind, cause, flow);
    this.callInfo = callInfo;
  }

  public @Nullable CallInfo getCallInfo() {
    return this.callInfo;
  }

  @Override
  public void createMessage(MessageCrafter messageCrafter) {
    final var definitionKey = getRulesDefinition().getDefinitionKey();
    final var calleeInfo = getCallInfo();

    CallInfo.createMessage(calleeInfo, definitionKey, messageCrafter);
    this.getCause().createMessage(messageCrafter);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ValueViolation that = (ValueViolation) o;
    return Objects.equals(callInfo, that.callInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), callInfo);
  }
}
