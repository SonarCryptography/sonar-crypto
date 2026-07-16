package org.sonarcrypto.ccerror.violations;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.FlowEntry;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class CallViolation extends Violation {

  public CallViolation(CryptoRulesDefinition rulesDefinition, Cause cause, List<FlowEntry> flow) {
    super(rulesDefinition, cause, flow);
  }

  public CallViolation(RuleKind ruleKind, Cause cause, List<FlowEntry> flow) {
    super(ruleKind, cause, flow);
  }

  @Override
  public void createMessage(MessageCrafter messageCrafter) {
    this.getCause().createMessage(messageCrafter);
  }
}
