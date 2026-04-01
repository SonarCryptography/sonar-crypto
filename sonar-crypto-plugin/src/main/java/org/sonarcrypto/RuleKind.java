package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum RuleKind {
  GENERAL,
  ALGORITHM,
  MODE,
  PADDING,
  KEY_LENGTH,
  FORBIDDEN_TYPE,
  FORBIDDEN_METHOD,
  UNCAUGHT_EXCEPTION,
  API_MISUSE
}
