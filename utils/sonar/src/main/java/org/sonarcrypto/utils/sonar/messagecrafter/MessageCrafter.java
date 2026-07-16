package org.sonarcrypto.utils.sonar.messagecrafter;

import java.util.ArrayList;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

@NullMarked
public final class MessageCrafter extends AbstractMessageCrafter {

  final StringBuilder buffer = new StringBuilder();
  final ArrayList<CraftedMessage.CodeRange> codeRanges = new ArrayList<>();

  @Override
  public MessageCrafter text(String text) {
    buffer.append(text);
    return this;
  }

  @Override
  public MessageCrafter code(String code) {
    if (code.isEmpty()) {
      return this;
    }

    final var bufferLength = buffer.length();
    codeRanges.add(new CraftedMessage.CodeRange(bufferLength, bufferLength + code.length()));
    buffer.append(code);
    return this;
  }

  @Override
  public MessageCrafter newLine() {
    return (MessageCrafter) super.newLine();
  }

  public MessageCrafter joining(
      @Nullable String lastDelimiter, Consumer<AbstractMessageCrafter> block) {
    return joining(", ", lastDelimiter, block);
  }

  public MessageCrafter joining(
      @Nullable String delimiter,
      @Nullable String lastDelimiter,
      Consumer<AbstractMessageCrafter> block) {
    final var joiner = new JoiningCrafter(this, delimiter, lastDelimiter);

    block.accept(joiner);
    joiner.finish();

    return this;
  }

  public AbstractMessageCrafter append(CraftedMessage message) {
    final var length = buffer.length();
    buffer.append(message.message());
    message
        .codeRanges()
        .forEach(
            otherRange ->
                codeRanges.add(
                    new CraftedMessage.CodeRange(
                        otherRange.start() + length, otherRange.end() + length)));
    return this;
  }

  @Override
  public String toString() {
    return buffer.toString();
  }

  public CraftedMessage toCraftedMessage() {
    return new CraftedMessage(buffer.toString(), new ArrayList<>(codeRanges));
  }

  public void addMessageTo(NewIssueLocation issueLocation) {
    issueLocation.message(
        buffer.toString(),
        codeRanges.stream()
            .map(
                it ->
                    issueLocation
                        .newMessageFormatting()
                        .start(it.start())
                        .end(it.end())
                        .type(MessageFormatting.Type.CODE))
            .toList());
  }
}
