package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum RuleKind {
  GENERAL,
  ALGORITHM,
  MODE,
  PADDING,
  KEY_MATERIAL,
  FORBIDDEN_METHOD,
  UNCAUGHT_EXCEPTION,
  API_MISUSE
}
