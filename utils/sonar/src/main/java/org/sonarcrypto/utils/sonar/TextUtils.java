package org.sonarcrypto.utils.sonar;

import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class TextUtils {

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public static String join(Iterable<?> values, @Nullable String lastDelimiter) {
    return join(values, lastDelimiter, null);
  }

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public static String join(
      Iterable<?> values, @Nullable String lastDelimiter, @Nullable String suffix) {
    return join(StreamSupport.stream(values.spliterator(), false), lastDelimiter, suffix);
  }

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public static String join(Stream<?> values, @Nullable String lastSeparator) {
    return join(values, lastSeparator, null);
  }

  /** Joins items of an iterable to a comma separated string, e.g. {@code "Foo, Bar, and Baz"}. */
  public static String join(
      Stream<?> values, @Nullable String lastSeparator, @Nullable String suffix) {
    final var iterator = values.iterator();

    if (!iterator.hasNext()) {
      return "";
    }

    final var sb = new StringBuilder();

    var valueCount = 0;

    while (iterator.hasNext()) {
      if (++valueCount > 1) {
        sb.append(", ");
      }

      final var next = iterator.next();

      if (valueCount > 2
          && lastSeparator != null
          && !lastSeparator.isEmpty()
          && !iterator.hasNext()) {
        sb.append(lastSeparator).append(' ');
      }

      if (next instanceof String s) {
        sb.append(quote(s));
      } else {
        sb.append(next);
      }
    }

    if (valueCount > 1 && suffix != null && !suffix.isEmpty()) {
      sb.append(", ").append(suffix);
    }

    return sb.toString();
  }

  /**
   * Quotes the given string and escapes special characters.
   *
   * <p>The value {@code "Foo \"Bar\"\nBaz"}, for example, becomes {@code "\"Foo
   * \\\"Bar\\\"\\nBaz\""}.
   *
   * @see StringEscapeUtils#escapeJava
   */
  public static String quote(String value) {
    return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
  }

  public static Code code(String value) {
    return new Code(
        "`" + StringEscapeUtils.escapeJava(value).replace("\\", "").replace("`", "``") + "`");
  }

  // We need a class, because records can only have public constructors
  public static final class Code {
    private final String code;

    Code(String code) {
      this.code = code;
    }

    @Override
    public String toString() {
      return code;
    }

    public String code() {
      return code;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Code) obj;
      return Objects.equals(this.code, that.code);
    }

    @Override
    public int hashCode() {
      return Objects.hash(code);
    }
  }
}
