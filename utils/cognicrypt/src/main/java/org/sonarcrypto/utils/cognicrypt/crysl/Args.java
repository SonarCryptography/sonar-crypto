package org.sonarcrypto.utils.cognicrypt.crysl;

import static java.util.function.Predicate.not;
import static org.sonarcrypto.utils.sonar.TextUtils.join;

import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record Args(List<String> actualValues, List<String> expectedValues) {
  public void createMessage(StringBuilder messageBuilder) {
    final var violatingValues = actualValues.stream().filter(not(String::isEmpty)).toList();
    final var violatingValuesCount = violatingValues.size();

    final var validValueRange = expectedValues;
    final var validValueRangeCount = validValueRange.size();

    if (violatingValuesCount > 0) {
      messageBuilder.append(violatingValuesCount == 1 ? "has the value " : "has the values ");
      messageBuilder.append(join(violatingValues, "or", "respectively"));
    }

    if (validValueRangeCount > 0) {
      if (violatingValuesCount == 0) {
        messageBuilder.append("should be ");
      } else if (violatingValuesCount == 1) {
        messageBuilder.append(", but it should be ");
      }
    } else {
      messageBuilder.append(", but they should be ");
    }

    if (validValueRangeCount > 1) {
      if (violatingValuesCount > 1) {
        messageBuilder.append("contained in ").append(join(validValueRange, "and"));
      } else {
        messageBuilder.append("one of ").append(join(validValueRange, "or"));
      }
    }

    messageBuilder.append('.');
  }
}
