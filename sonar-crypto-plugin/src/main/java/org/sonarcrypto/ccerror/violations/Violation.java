package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public sealed interface Violation permits ArgsViolation, SimpleViolation {
  CryptoRulesDefinition rulesDefinition();

  void createMessage(StringBuilder messageBuilder);
}
