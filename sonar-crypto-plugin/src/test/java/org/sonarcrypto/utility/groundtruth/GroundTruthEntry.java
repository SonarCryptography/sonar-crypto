package org.sonarcrypto.utility.groundtruth;

import static org.sonarcrypto.utils.sonar.TextUtils.quote;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.causes.Cause;

@NullMarked
public record GroundTruthEntry(
    RuleKind ruleKind, Class<? extends Cause> causeType, @Nullable String value) {
  @Override
  public String toString() {
    final var sb =
        new StringBuilder()
            .append(ruleKind)
            .append('/')
            .append(GroundTruthUtils.toString(causeType));

    if (value != null) {
      sb.append(' ').append(quote(value));
    }

    return sb.toString();
  }
}
