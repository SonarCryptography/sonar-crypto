package org.sonarcrypto.utils.sonar.messagecrafter;

import static org.sonarcrypto.utils.sonar.SignatureUtils.shortNameOf;
import static org.sonarcrypto.utils.sonar.TextUtils.escape;

import boomerang.scope.DeclaredMethod;
import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import crypto.utils.CrySLUtils;
import crysl.rule.CrySLMethod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;
import org.sonarcrypto.utils.cognicrypt.crysl.CallInfo;
import org.sonarcrypto.utils.sonar.FqClassName;

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

    final var escapedCode = escape(code);
    final var bufferLength = buffer.length();
    codeRanges.add(new CraftedMessage.CodeRange(bufferLength, bufferLength + escapedCode.length()));
    buffer.append(escapedCode);
    return this;
  }

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public MessageCrafter codeJoined(Iterable<?> values, @Nullable String lastDelimiter) {
    return codeJoined(values, lastDelimiter, null);
  }

  /**
   * Joins items of an iterable to a comma separated string,e.g. {@code "Foo, Bar, or Baz,
   * respectively"}.
   */
  public MessageCrafter codeJoined(
      Iterable<?> values, @Nullable String lastDelimiter, @Nullable String suffix) {
    return codeJoined(StreamSupport.stream(values.spliterator(), false), lastDelimiter, suffix);
  }

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public MessageCrafter codeJoined(Stream<?> values, @Nullable String lastSeparator) {
    return codeJoined(values, lastSeparator, null);
  }

  /**
   * Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, or Baz,
   * respectively"}.
   */
  public MessageCrafter codeJoined(
      Stream<?> values, @Nullable String lastDelimiter, @Nullable String suffix) {
    return joining(
        lastDelimiter,
        joiner -> {
          final var count =
              new Object() {
                int value = 0;
              };

          values.forEach(
              value -> {
                count.value++;
                joiner.code(value.toString());
              });

          if (count.value > 1 && suffix != null && !suffix.isEmpty()) {
            text(suffix);
          }
        });
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

  public MessageCrafter method(DeclaredMethod method) {
    return method(method.getDeclaringClass(), method.getName());
  }

  public MessageCrafter method(Method method) {
    return method(method.getDeclaringClass(), method.getName());
  }

  public MessageCrafter method(FqClassName declaringClass, String methodName) {
    return method(declaringClass.fqn(), methodName);
  }

  public MessageCrafter method(WrappedClass declaringClass, String methodName) {
    return method(declaringClass.getFullyQualifiedName(), methodName);
  }

  public MessageCrafter method(WrappedClass clazz) {
    return method(clazz.getFullyQualifiedName(), null);
  }

  public MessageCrafter method(FqClassName clazz) {
    return method(clazz.fqn(), null);
  }

  public MessageCrafter method(String fqn) {
    return method(fqn, null);
  }

  public MessageCrafter method(String declaringClassFqn, @Nullable String methodName) {
    return code(shortNameOf(declaringClassFqn, methodName));
  }

  public MessageCrafter methodsJoined(
      @Nullable String prefixWhenMultiple,
      Collection<CrySLMethod> methods,
      @Nullable String lastDelimiter) {
    final var collected =
        methods.stream()
            .map(it -> shortNameOf(it.getDeclaringClassName(), it.getMethodName()))
            .collect(Collectors.toSet());

    if (collected.size() > 1 && prefixWhenMultiple != null) {
      text(prefixWhenMultiple).text(" ");
    }

    return joining(lastDelimiter, joiner -> collected.forEach(joiner::code));
  }

  /**
   * Stringifies the callee from a callee info, e.g., {@code `Foo.bar`}. Constructor and static
   * constructor are handled differently, e.g., {@code `Foo`'s constructor}. If the callee info is
   * {@code null}, the string {@code the callee} is returned.
   */
  public MessageCrafter callee(@Nullable CalleeInfo calleeInfo) {
    if (calleeInfo == null) {
      return text("the callee");
    }

    return switch (calleeInfo.methodName()) {
      case "<init>" -> code(shortNameOf(calleeInfo.className(), null)).text("'s constructor");
      case "<clinit>" ->
          code(shortNameOf(calleeInfo.className(), null)).text("'s static constructor");
      default -> code(shortNameOf(calleeInfo.className(), calleeInfo.methodName()));
    };
  }

  public void callInfo(@Nullable CallInfo callInfo, @Nullable String key) {
    if (callInfo != null) {
      final var calleeInfo = callInfo.calleeInfo();

      text("The ")
          .text(key != null ? key : "value")
          .text(", given as ")
          .argumentIndex(
              callInfo.argumentIndex(), calleeInfo != null ? calleeInfo.argumentCount() : -1)
          .text(" to ")
          .callee(calleeInfo)
          .text(" ");
    } else {
      text("The ").text(key != null ? key : "value").text(", given as argument, ");
    }
  }

  /**
   * Converts a zero-based argument index into an ordinal form.
   *
   * <table>
   *     <tr>
   *         <th>Index</th>
   *         <th>Result</th>
   *     </tr>
   *     <tr>
   *         <td><code>&lt; 0 || argumentCount == 1</code></td>
   *         <td>just <code>"argument"</code></td>
   *     </tr>
   *     <tr>
   *         <td><code>&gt; 0 && &lt; 6</code></td>
   *         <td>ordinal as a word, e.g., <code>"third argument"</code></td>
   *     </tr>
   *     <tr>
   *         <td><i>else</i></td>
   *         <td>ordinal as a number, e.g., <code>"7th argument"</code></td>
   *     </tr>
   * </table>
   *
   * <p>See also {@link CrySLUtils#getIndexAsString} for parameters that also considers a negative
   * index as "return value".
   */
  public MessageCrafter argumentIndex(int zeroBasedArgumentIndex, int parameterCount) {
    if (zeroBasedArgumentIndex < 0 || parameterCount == 1) return text("argument");

    return text(
        switch (zeroBasedArgumentIndex) {
          case 0 -> "first argument";
          case 1 -> "second argument";
          case 2 -> "third argument";
          case 3 -> "fourth argument";
          case 4 -> "fifth argument";
          case 5 -> "sixth argument";
          default -> (zeroBasedArgumentIndex + 1) + "th argument";
        });
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
