package org.sonarcrypto.utils.sonar.messagecrafter;

import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract sealed class AbstractMessageCrafter permits JoiningCrafter, MessageCrafter {
  public abstract AbstractMessageCrafter text(String text);

  public abstract AbstractMessageCrafter code(String code);

  public AbstractMessageCrafter newLine() {
    return this.text(System.lineSeparator());
  }
}
