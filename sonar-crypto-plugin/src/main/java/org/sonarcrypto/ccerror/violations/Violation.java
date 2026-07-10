package org.sonarcrypto.ccerror.violations;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.CryptoRulesDefinitions;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.FlowEntry;
import org.sonarcrypto.ccerror.causes.Cause;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public abstract sealed class Violation permits ValueViolation, CallViolation {
  private final CryptoRulesDefinition rulesDefinition;

  private final Cause cause;
  private final List<FlowEntry> flow;

  protected Violation(CryptoRulesDefinition rulesDefinition, Cause cause, List<FlowEntry> flow) {
    this.rulesDefinition = rulesDefinition;
    this.cause = cause;
    this.flow = flow;
  }

  protected Violation(RuleKind ruleKind, Cause cause, List<FlowEntry> flow) {
    this(CryptoRulesDefinitions.fromRuleKind(ruleKind), cause, flow);
  }

  public CryptoRulesDefinition getRulesDefinition() {
    return this.rulesDefinition;
  }

  public Cause getCause() {
    return this.cause;
  }

  public List<FlowEntry> getFlow() {
    return this.flow;
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
