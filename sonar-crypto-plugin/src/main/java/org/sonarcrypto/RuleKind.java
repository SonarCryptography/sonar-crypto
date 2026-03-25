package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum RuleKind {
  GENERAL,
  ALGORITHM,
  // MODE,
  // PADDING,
  KEY_LENGTH,
  FORBIDDEN_TYPE;

  public String getRuleId() {
    return "CC" + (ordinal() + 1);
  }
}
