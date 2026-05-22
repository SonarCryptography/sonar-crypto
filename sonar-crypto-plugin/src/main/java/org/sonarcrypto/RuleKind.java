package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;

/**
 * Defines the rule kinds for the categorization in SonarQube.
 *
 * <p><i><b>Important:</b> The rule identifiers that are registered in SonarQube, such as
 * <tt>"CC1"</tt>, are taken from the elements' ordinals; hence, the order should never be changed.
 * <b>Add new elements exclusively to the <u>bottom of the list</u>.</b></i>
 */
@NullMarked
public enum RuleKind {
  GENERAL,
  ALGORITHM,
  MODE,
  PADDING,
  KEY_MATERIAL,
  FORBIDDEN_METHOD,
  UNCAUGHT_EXCEPTION,
  API_MISUSE;

  /**
   * Returns the rule identifier that is registered in SonarQube, e.g., <tt>"CC1"</tt>. The number
   * part of the identifier, starting at <tt>1</tt>, is based on the element's ordinal.
   */
  public String toSQRuleName() {
    return "CC" + (ordinal() + 1);
  }
}
