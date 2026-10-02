package org.sonarcrypto.analysis;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * Typed key for a value in {@link CryptoAnalysisInfo}.
 *
 * <p>Each metric knows how to convert its value into a portable form (strings, numbers and flat
 * maps of those). Metrics can only be created through the factory methods below, which keeps every
 * value serializable. All metrics are defined in {@link MetricDefinitions}.
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

  static <E extends Enum<E>> Metric<Map<E, Integer>> ofEnumCounts(String name) {
    return new Metric<>(
        name,
        counts -> {
          var byName = new LinkedHashMap<String, Integer>();
          counts.forEach((key, count) -> byName.put(key.name(), count));
          return byName;
        });
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
