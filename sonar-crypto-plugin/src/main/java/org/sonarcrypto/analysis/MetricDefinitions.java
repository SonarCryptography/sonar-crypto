package org.sonarcrypto.analysis;

import static org.sonarcrypto.analysis.Metric.ofEnum;
import static org.sonarcrypto.analysis.Metric.ofEnumCounts;
import static org.sonarcrypto.analysis.Metric.ofNumber;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.RuleKind;

/**
 * All metrics collected per analysis run. Together they form the layout of the {@code metrics}
 * object written by {@link CryptoAnalysisInfo#writeJson}, so bump {@link
 * CryptoAnalysisInfo#SCHEMA_VERSION} when renaming or removing one.
 */
@NullMarked
public final class MetricDefinitions {

  public static final Metric<InputSource> INPUT_SOURCE = ofEnum("inputSource");
  public static final Metric<Long> COMPILE_MILLIS = ofNumber("compileMillis");
  public static final Metric<Long> ANALYSIS_MILLIS = ofNumber("analysisMillis");
  public static final Metric<Integer> TOTAL_ERRORS = ofNumber("totalErrors");
  public static final Metric<Map<RuleKind, Integer>> ERRORS_PER_RULE_KIND =
      ofEnumCounts("errorsPerRuleKind");
  public static final Metric<Integer> CLASSES_ANALYZED = ofNumber("classesAnalyzed");
  public static final Metric<Integer> METHODS_ANALYZED = ofNumber("methodsAnalyzed");

  private MetricDefinitions() {}
}
