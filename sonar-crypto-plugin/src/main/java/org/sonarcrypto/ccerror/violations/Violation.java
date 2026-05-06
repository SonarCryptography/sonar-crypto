package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.violations.reasons.Reason;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public abstract sealed class Violation permits ArgViolation, CallViolation {
  private final CryptoRulesDefinition rulesDefinition;

  private final Reason reason;

  public Violation(CryptoRulesDefinition rulesDefinition, Reason reason) {
    this.rulesDefinition = rulesDefinition;
    this.reason = reason;
  }

  public Violation(RuleKind ruleKind, Reason reason) {
    this.rulesDefinition = CryptoRulesDefinitions.fromRuleKind(ruleKind);
    this.reason = reason;
  }

  public CryptoRulesDefinition getRulesDefinition() {
    return this.rulesDefinition;
  }

  public Reason getReason() {
    return this.reason;
  }

  public abstract void createMessage(StringBuilder messageBuilder);

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Violation violation = (Violation) o;
    return rulesDefinition.equals(violation.rulesDefinition) && reason.equals(violation.reason);
  }

  @Override
  public int hashCode() {
    int result = rulesDefinition.hashCode();
    result = 31 * result + reason.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Violation{" + "rulesDefinition=" + rulesDefinition + ", reason=" + reason + '}';
  }
}
