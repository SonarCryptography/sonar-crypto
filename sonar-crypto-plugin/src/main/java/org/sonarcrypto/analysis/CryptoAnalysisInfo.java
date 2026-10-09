package org.sonarcrypto.analysis;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Aggregated information about a single CogniCrypt analysis run, stored as typed {@link Metric}s in
 * insertion order.
 */
@NullMarked
public final class CryptoAnalysisInfo {

  /** Version of the JSON layout written by {@link #writeJson(Path)}; bump on breaking changes. */
  public static final int SCHEMA_VERSION = 2;

  private final Map<Metric<?>, Object> values = new LinkedHashMap<>();

  public <T> CryptoAnalysisInfo put(Metric<T> metric, T value) {
    values.put(metric, value);
    return this;
  }

  /**
   * Runs {@code action} and stores its duration in milliseconds under {@code metric}, also when the
   * action throws.
   */
  public <E extends Exception> void time(Metric<Long> metric, TimedRunnable<E> action) throws E {
    timeAndGet(
        metric,
        () -> {
          action.run();
          return null;
        });
  }

  /** Like {@link #time(Metric, TimedRunnable)}, but returns the action's result. */
  public <T, E extends Exception> T timeAndGet(Metric<Long> metric, TimedSupplier<T, E> action)
      throws E {
    final long start = System.nanoTime();
    try {
      return action.get();
    } finally {
      put(metric, (System.nanoTime() - start) / 1_000_000);
    }
  }

  @SuppressWarnings("unchecked") // put() only stores values matching the metric's type
  public <T> @Nullable T get(Metric<T> metric) {
    return (T) values.get(metric);
  }

  /**
   * All metrics in insertion order, keyed by name, with values converted to strings, numbers,
   * booleans and flat maps of those, so the result can be serialized as-is by any JSON/YAML/CSV
   * writer.
   */
  public Map<String, Object> asMap() {
    var portable = new LinkedHashMap<String, Object>();
    values.forEach((metric, value) -> portable.put(metric.name(), toPortable(metric, value)));
    return portable;
  }

  /**
   * Writes all metrics as JSON to {@code file}, wrapped in an envelope with the schema version and
   * a timestamp. Missing parent directories are created.
   */
  public void writeJson(Path file) throws IOException {
    var envelope = new LinkedHashMap<String, Object>();
    envelope.put("schemaVersion", SCHEMA_VERSION);
    envelope.put("timestamp", Instant.now().toString());
    envelope.put("metrics", asMap());

    var parent = file.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    try (var writer = Files.newBufferedWriter(file)) {
      new GsonBuilder().setPrettyPrinting().create().toJson(envelope, writer);
    }
  }

  @SuppressWarnings("unchecked") // put() only stores values matching the metric's type
  private static <T> Object toPortable(Metric<T> metric, Object value) {
    return metric.toPortable((T) value);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder("Crypto analysis summary:");
    asMap().forEach((name, value) -> sb.append("\n  ").append(name).append('=').append(value));
    return sb.toString();
  }

  /** Action timed by {@link #time(Metric, TimedRunnable)}. */
  @FunctionalInterface
  public interface TimedRunnable<E extends Exception> {
    void run() throws E;
  }

  /** Action timed by {@link #timeAndGet(Metric, TimedSupplier)}. */
  @FunctionalInterface
  public interface TimedSupplier<T, E extends Exception> {
    T get() throws E;
  }
}
