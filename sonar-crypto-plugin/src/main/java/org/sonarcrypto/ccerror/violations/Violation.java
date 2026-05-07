package org.sonarcrypto.ccerror.violations;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public abstract sealed class Violation permits ValueViolation, CallViolation {
  private final CryptoRulesDefinition rulesDefinition;

  private final Cause cause;

  public Violation(CryptoRulesDefinition rulesDefinition, Cause cause) {
    this.rulesDefinition = rulesDefinition;
    this.cause = cause;
  }

  public Violation(RuleKind ruleKind, Cause cause) {
    this.rulesDefinition = CryptoRulesDefinitions.fromRuleKind(ruleKind);
    this.cause = cause;
  }

  public CryptoRulesDefinition getRulesDefinition() {
    return this.rulesDefinition;
  }

  public Cause getReason() {
    return this.cause;
  }

  public abstract void createMessage(StringBuilder messageBuilder);

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Violation violation = (Violation) o;
    return rulesDefinition.equals(violation.rulesDefinition) && cause.equals(violation.cause);
  }

  @Override
  public int hashCode() {
    int result = rulesDefinition.hashCode();
    result = 31 * result + cause.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Violation{" + "rulesDefinition=" + rulesDefinition + ", cause=" + cause + '}';
  }
}
