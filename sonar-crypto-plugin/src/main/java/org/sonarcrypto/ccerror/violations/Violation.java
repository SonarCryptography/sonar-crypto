package org.sonarcrypto.ccerror.violations;

import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

public interface Violation {
  CryptoRulesDefinition rulesDefinition();

  void createMessage(StringBuilder messageBuilder);
}
