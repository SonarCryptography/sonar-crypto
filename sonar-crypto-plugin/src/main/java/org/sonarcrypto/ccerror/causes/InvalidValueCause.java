package org.sonarcrypto.ccerror.causes;

import static java.util.function.Predicate.not;
import static org.sonarcrypto.utils.sonar.TextUtils.join;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class InvalidValueCause extends ValueCause {

  private final List<String> actualValues;
  private final List<String> expectedValues;

  public InvalidValueCause(final List<String> actualValues, final List<String> expectedValues) {
    this.actualValues = actualValues;
    this.expectedValues = expectedValues;
  }

  public List<String> getActualValues() {
    return this.actualValues;
  }

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

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InvalidValueCause that = (InvalidValueCause) o;
    return actualValues.equals(that.actualValues) && expectedValues.equals(that.expectedValues);
  }

  @Override
  public int hashCode() {
    int result = actualValues.hashCode();
    result = 31 * result + expectedValues.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "InvalidValueCause{"
        + "actualValues="
        + actualValues
        + ", expectedValues="
        + expectedValues
        + '}';
  }
}
