package org.sonarcrypto.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonarcrypto.RuleKind;

/**
 * SonarQube custom measures exposing aggregated crypto analysis information.
 *
 * <p>Measures are only registered (and saved) when {@link #isEnabled()} returns {@code true}. This
 * is controlled by the system property {@code sonarcrypto.measures.enabled} or, as a fallback, the
 * environment variable {@code SONARCRYPTO_MEASURES_ENABLED}. Because metric registration happens on
 * the server while values are saved from the scanner, the flag must be set on both JVMs in a real
 * deployment (e.g. {@code sonar.web.javaAdditionalOpts=-Dsonarcrypto.measures.enabled=true}).
 *
 * <p>To add a new measure, add a field to {@link CryptoAnalysisInfo} and one {@link MetricDef}
 * entry to {@link #DEFINITIONS}. Saving, summarizing, and registration iterate over that list
 * generically and do not need to change.
 */
@NullMarked
public class CryptoMetrics implements Metrics {

  private static final String ENABLED_PROPERTY = "sonarcrypto.measures.enabled";
  private static final String ENABLED_ENV = "SONARCRYPTO_MEASURES_ENABLED";

  public static final Metric<Long> COMPILE_RUNTIME =
      metric(
          "crypto.analysis.compile.runtime",
          "Crypto analysis compile time",
          Metric.ValueType.MILLISEC);
  public static final Metric<Long> ANALYSIS_RUNTIME =
      metric("crypto.analysis.runtime", "Crypto analysis time", Metric.ValueType.MILLISEC);
  public static final Metric<Integer> ERRORS_TOTAL =
      metric("crypto.errors.total", "Number of crypto errors", Metric.ValueType.INT);
  public static final Metric<Integer> CLASSES_ANALYZED =
      metric("crypto.classes.analyzed", "Classes analyzed", Metric.ValueType.INT);
  public static final Metric<Integer> METHODS_ANALYZED =
      metric("crypto.methods.analyzed", "Methods analyzed", Metric.ValueType.INT);
  public static final Metric<String> INPUT_SOURCE =
      metric("crypto.input.source", "Analysis input source", Metric.ValueType.STRING);

  private static final Map<RuleKind, Metric<Integer>> RULE_KIND_METRICS = ruleKindMetrics();

  private record MetricDef<G extends Serializable>(
      Metric<G> metric, Function<CryptoAnalysisInfo, G> value) {}

  private static final List<MetricDef<?>> DEFINITIONS = definitions();

  /** Whether crypto analysis measures are enabled (off by default). */
  public static boolean isEnabled() {
    var value = System.getProperty(ENABLED_PROPERTY);
    if (value == null) {
      value = System.getenv(ENABLED_ENV);
    }
    return Boolean.parseBoolean(value);
  }

  /** Saves all defined measures to SonarQube for the given analysis info. */
  public static void save(SensorContext context, CryptoAnalysisInfo info) {
    for (var definition : DEFINITIONS) {
      save(context, definition, info);
    }
  }

  /** Renders a human-readable, one-line-per-measure summary of the analysis info. */
  public static String summarize(CryptoAnalysisInfo info) {
    var builder = new StringBuilder("Crypto analysis summary:");
    for (var definition : DEFINITIONS) {
      builder
          .append("\n  ")
          .append(definition.metric().getKey())
          .append('=')
          .append(definition.value().apply(info));
    }
    return builder.toString();
  }

  @Override
  public List<Metric> getMetrics() {
    return DEFINITIONS.stream().map(MetricDef::metric).map(metric -> (Metric) metric).toList();
  }

  private static <G extends Serializable> void save(
      SensorContext context, MetricDef<G> definition, CryptoAnalysisInfo info) {
    context
        .<G>newMeasure()
        .forMetric(definition.metric())
        .on(context.module())
        .withValue(definition.value().apply(info))
        .save();
  }

  private static List<MetricDef<?>> definitions() {
    var definitions = new ArrayList<MetricDef<?>>();
    definitions.add(new MetricDef<>(COMPILE_RUNTIME, CryptoAnalysisInfo::compileMillis));
    definitions.add(new MetricDef<>(ANALYSIS_RUNTIME, CryptoAnalysisInfo::analysisMillis));
    definitions.add(new MetricDef<>(ERRORS_TOTAL, CryptoAnalysisInfo::totalErrors));
    definitions.add(new MetricDef<>(CLASSES_ANALYZED, CryptoAnalysisInfo::classesAnalyzed));
    definitions.add(new MetricDef<>(METHODS_ANALYZED, CryptoAnalysisInfo::methodsAnalyzed));
    definitions.add(new MetricDef<>(INPUT_SOURCE, info -> info.inputSource().name()));
    for (var entry : RULE_KIND_METRICS.entrySet()) {
      var kind = entry.getKey();
      definitions.add(
          new MetricDef<>(
              entry.getValue(), info -> info.errorsPerRuleKind().getOrDefault(kind, 0)));
    }
    return List.copyOf(definitions);
  }

  private static Map<RuleKind, Metric<Integer>> ruleKindMetrics() {
    var metrics = new EnumMap<RuleKind, Metric<Integer>>(RuleKind.class);
    for (var kind : RuleKind.values()) {
      var name = kind.name().toLowerCase();
      metrics.put(
          kind,
          metric("crypto.errors." + name, "Crypto errors (" + name + ")", Metric.ValueType.INT));
    }
    return metrics;
  }

  private static <G extends Serializable> Metric<G> metric(
      String key, String name, Metric.ValueType type) {
    return new Metric.Builder(key, name, type).setDescription(name).create();
  }
}
