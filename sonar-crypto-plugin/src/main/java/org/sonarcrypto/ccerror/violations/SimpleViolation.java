package org.sonarcrypto.ccerror.violations;

import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;
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

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static SimpleViolation general(
      Optional<CalleeInfo> calleeInfo, Optional<Integer> argumentIndex) {
    return general(calleeInfo, argumentIndex.orElse(-1), null);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static SimpleViolation general(
      Optional<CalleeInfo> calleeInfo, @Nullable String subMessage) {
    return general(calleeInfo, -1, subMessage);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static SimpleViolation general(
      Optional<CalleeInfo> calleeInfo, int argumentIndex, @Nullable String subMessage) {
    return new SimpleViolation(
        CryptoRulesDefinitions.CC1_GENERAL,
        CallInfo.optOf(calleeInfo, argumentIndex),
        subMessage != null ? subMessage : "was cryptographically improper generated.");
  }
}
