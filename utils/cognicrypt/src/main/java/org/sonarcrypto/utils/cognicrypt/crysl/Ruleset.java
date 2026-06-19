package org.sonarcrypto.utils.cognicrypt.crysl;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum Ruleset {

  /** Bouncy Castle with its own API */
  BC("bc"),

  /** Bouncy Castle for JCA */
  BC_JCA("bc-jca"),

  /** JCA */
  JCA("jca"),

  /** JCA with Bouncy Castle for JCA */
  JCA_BC_JCA("jca-bc-jca"),

  /** Google Tink with its own API */
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
