package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum Ruleset {

  /** Bouncy Castle */
  BC("bc"),

  /** Bouncy Castle and JCA */
  BC_JCA("bc-jca"),

  /** JCA */
  JCA("jca"),

  /** Google Tink */
  TINK("tink");

  private final String rulesetName;

  Ruleset(String rulesetName) {
    this.rulesetName = rulesetName;
  }

  /** Gets the ruleset name. */
  public String getRulesetName() {
    return rulesetName;
  }
}
