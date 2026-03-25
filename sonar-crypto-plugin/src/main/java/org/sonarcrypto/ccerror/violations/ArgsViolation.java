package org.sonarcrypto.ccerror.violations;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.crysl.Args;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public record ArgsViolation(
    CryptoRulesDefinition rulesDefinition, Optional<CallInfo> callInfo, Args args)
    implements Violation {

  public ArgsViolation(RuleKind ruleKind, Optional<CallInfo> callInfo, Args args) {
    this(CryptoRulesDefinitions.fromRuleKind(ruleKind), callInfo, args);
  }

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    CallInfo.createMessage(callInfo, rulesDefinition.getDefinitionKey(), messageBuilder);
    args.createMessage(messageBuilder);
  }
}
