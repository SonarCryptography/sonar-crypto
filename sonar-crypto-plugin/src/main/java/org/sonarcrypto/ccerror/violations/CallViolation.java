package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public final class CallViolation extends Violation {

  public CallViolation(CryptoRulesDefinition rulesDefinition, Cause cause) {
    super(rulesDefinition, cause);
  }

  public CallViolation(RuleKind ruleKind, Cause cause) {
    super(ruleKind, cause);
  }

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    this.getCause().createMessage(messageBuilder);
  }
}
