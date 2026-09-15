package org.sonarcrypto.analysis;

import org.jspecify.annotations.NullMarked;

/** Identifies which input CogniCrypt analyzed. */
@NullMarked
public enum InputSource {
  JIMPLE,
  MAVEN
}
