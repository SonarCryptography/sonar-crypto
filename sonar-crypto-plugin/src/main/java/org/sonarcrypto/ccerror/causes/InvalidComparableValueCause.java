package org.sonarcrypto.ccerror.causes;

import crysl.rule.CrySLComparisonConstraint.CompOp;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

@NullMarked
public final class InvalidComparableValueCause extends ValueCause {

  private final String leftOperand;
  private final CompOp operator;
  private final String rightOperand;

  public InvalidComparableValueCause(
      final String leftOperand, final CompOp operator, final String rightOperand) {
    this.leftOperand = leftOperand;
    this.operator = operator;
    this.rightOperand = rightOperand;
  }

  public String getLeftOperand() {
    return this.leftOperand;
  }

  public CompOp getOperator() {
    return this.operator;
  }

  public String getRightOperand() {
    return this.rightOperand;
  }

  @Override
  public void createMessage(MessageCrafter messageCrafter) {
    messageCrafter.text("has an invalid value: ");

    final var operator =
        switch (getOperator()) {
          case l -> "<";
          case g -> ">";
          case le -> "<=";
          case ge -> ">=";
          case eq -> "==";
          case neq -> "!=";
        };

    messageCrafter.code(getLeftOperand());
    messageCrafter.text(" is expected to be ");
    messageCrafter.code(operator + " " + getRightOperand());
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    InvalidComparableValueCause that = (InvalidComparableValueCause) o;
    return Objects.equals(leftOperand, that.leftOperand)
        && operator == that.operator
        && Objects.equals(rightOperand, that.rightOperand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(leftOperand, operator, rightOperand);
  }

  @Override
  public String toString() {
    return "InvalidComparableValueCause{"
        + "leftOperand='"
        + leftOperand
        + '\''
        + ", operator="
        + operator
        + ", rightOperand='"
        + rightOperand
        + '\''
        + '}';
  }
}
