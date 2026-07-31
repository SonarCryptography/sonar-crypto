package org.sonarcrypto.utils.sonar.messagecrafter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class JoiningCrafter extends AbstractMessageCrafter {

  private final AbstractMessageCrafter crafter;

  private boolean hasWritten;
  private final String delimiter;
  private final String lastDelimiter;

  @Nullable private String lastElement;
  private boolean lastElementIsCode;

  public JoiningCrafter(
      final AbstractMessageCrafter crafter,
      final @Nullable String delimiter,
      final @Nullable String lastDelimiter) {
    this.crafter = crafter;
    this.delimiter = delimiter == null ? ", " : delimiter;
    this.lastDelimiter = this.delimiter + (lastDelimiter == null ? "" : lastDelimiter + " ");
  }

  private void appendLastElement(boolean isFinish) {
    if (lastElement == null) {
      return;
    }

    if (hasWritten) {
      crafter.text(isFinish ? lastDelimiter : delimiter);
    }

    this.hasWritten = true;

    if (lastElementIsCode) {
      crafter.code(lastElement);
    } else {
      crafter.text(lastElement);
    }

    lastElement = null;
  }

  @Override
  public AbstractMessageCrafter text(String text) {
    if (text.isEmpty()) {
      return this;
    }

    appendLastElement(false);

    lastElement = text;
    lastElementIsCode = false;

    return this;
  }

  @Override
  public AbstractMessageCrafter code(String code) {
    appendLastElement(false);

    lastElement = code;
    lastElementIsCode = true;

    return this;
  }

  void finish() {
    appendLastElement(true);
  }
}
