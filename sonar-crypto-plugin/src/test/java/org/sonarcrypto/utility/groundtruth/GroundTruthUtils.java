package org.sonarcrypto.utility.groundtruth;

import org.sonarcrypto.ccerror.causes.Cause;

public class GroundTruthUtils {
  public static String toString(Class<? extends Cause> causeType) {
    final var simpleName = causeType.getSimpleName();
    return simpleName.substring(0, simpleName.length() - 5);
  }
}
