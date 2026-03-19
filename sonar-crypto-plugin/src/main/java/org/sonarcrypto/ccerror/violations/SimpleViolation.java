package org.sonarcrypto.ccerror.violations;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public record SimpleViolation(
    CryptoRulesDefinition rulesDefinition, Optional<CallInfo> callInfo, String subMessage)
    implements Violation {

  @Override
  public void createMessage(StringBuilder messageBuilder) {
    CallInfo.createMessage(callInfo, rulesDefinition.getDefinitionKey(), messageBuilder);
    messageBuilder.append(subMessage);
  }
}
