package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.violations.reasons.Reason;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public final class CallViolation extends Violation {

  public CallViolation(CryptoRulesDefinition rulesDefinition, Reason reason) {
    super(rulesDefinition, reason);
  }

  public CallViolation(RuleKind ruleKind, Reason reason) {
    super(ruleKind, reason);
  }

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    this.getReason().createMessage(messageBuilder);
  }
}
