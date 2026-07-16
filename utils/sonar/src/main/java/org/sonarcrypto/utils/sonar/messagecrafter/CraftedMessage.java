package org.sonarcrypto.utils.sonar.messagecrafter;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

@NullMarked
public record CraftedMessage(String message, List<CodeRange> codeRanges) {

  public CraftedMessage(String message) {
    this(message, List.of(/* empty */ ));
  }

  public void addMessageTo(NewIssueLocation issueLocation) {
    issueLocation.message(
        message,
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

  public record CodeRange(int start, int end) {}
}
