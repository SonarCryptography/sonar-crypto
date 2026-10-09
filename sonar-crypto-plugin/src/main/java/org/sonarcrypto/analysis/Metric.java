package org.sonarcrypto.analysis;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * Typed key for a value in {@link CryptoAnalysisInfo}.
 *
 * <p>Each metric knows how to convert its value into a portable form (strings, numbers, booleans
 * and flat maps of those). Metrics can only be created through the factory methods below, which
 * keeps every value serializable. All metrics are defined in {@link MetricDefinitions}.
 */
@NullMarked
public final class Metric<T> {

  private final String name;
  private final Function<T, Object> toPortable;

  private Metric(String name, Function<T, Object> toPortable) {
    this.name = name;
    this.toPortable = toPortable;
  }

  static <N extends Number> Metric<N> ofNumber(String name) {
    return new Metric<>(name, value -> value);
  }

  static <E extends Enum<E>> Metric<E> ofEnum(String name) {
    return new Metric<>(name, Enum::name);
  }

  static Metric<Boolean> ofBoolean(String name) {
    return new Metric<>(name, value -> value);
  }

  static Metric<String> ofString(String name) {
    return new Metric<>(name, value -> value);
  }

  /** Counts per enum constant; constants without a count are written as 0. */
  static <E extends Enum<E>> Metric<Map<E, Integer>> ofEnumCounts(String name, Class<E> type) {
    return new Metric<>(
        name,
        counts -> {
          var byName = new LinkedHashMap<String, Integer>();
          for (var constant : type.getEnumConstants()) {
            byName.put(constant.name(), counts.getOrDefault(constant, 0));
          }
          return byName;
        });
  }

  /** Counts per arbitrary key, written sorted by key. */
  static Metric<Map<String, Integer>> ofCounts(String name) {
    return new Metric<>(name, TreeMap::new);
  }

  public String name() {
    return name;
  }

  Object toPortable(T value) {
    return toPortable.apply(value);
  }

  @Override
  public String toString() {
    return name;
  }
}
