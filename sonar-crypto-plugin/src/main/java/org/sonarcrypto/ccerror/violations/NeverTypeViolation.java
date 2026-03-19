package org.sonarcrypto.ccerror.violations;

import static org.sonarcrypto.utils.sonar.TextUtils.quote;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;

@NullMarked
public record NeverTypeViolation(
    CryptoRulesDefinition rulesDefinition, Optional<CallInfo> callInfo, String notAllowedType)
    implements Violation {
  @Override
  public void createMessage(StringBuilder messageBuilder) {
    CallInfo.createMessage(callInfo, "value", messageBuilder);
    messageBuilder.append("should never be of the type ").append(quote(notAllowedType)).append('.');
  }
}
