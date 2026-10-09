package org.sonarcrypto.analysis;

import org.jspecify.annotations.NullMarked;

/** How far an analysis run got before it finished or failed. */
@NullMarked
public enum Outcome {
  SUCCESS,
  RULE_EXTRACTION_FAILED,
  COMPILE_FAILED,
  ANALYSIS_FAILED
}
