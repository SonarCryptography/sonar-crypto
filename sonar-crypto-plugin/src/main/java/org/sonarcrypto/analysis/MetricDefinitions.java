package org.sonarcrypto.analysis;

import static org.sonarcrypto.analysis.Metric.ofBoolean;
import static org.sonarcrypto.analysis.Metric.ofCounts;
import static org.sonarcrypto.analysis.Metric.ofEnum;
import static org.sonarcrypto.analysis.Metric.ofEnumCounts;
import static org.sonarcrypto.analysis.Metric.ofNumber;
import static org.sonarcrypto.analysis.Metric.ofString;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.utils.cognicrypt.crysl.Ruleset;

/**
 * All metrics collected per analysis run. Together they form the layout of the {@code metrics}
 * object written by {@link CryptoAnalysisInfo#writeJson}, so bump {@link
 * CryptoAnalysisInfo#SCHEMA_VERSION} when renaming or removing one.
 */
@NullMarked
public final class MetricDefinitions {

  public static final Metric<String> PROJECT_KEY = ofString("projectKey");
  public static final Metric<String> PLUGIN_VERSION = ofString("pluginVersion");
  public static final Metric<String> COGNICRYPT_VERSION = ofString("cognicryptVersion");
  public static final Metric<Ruleset> RULESET = ofEnum("ruleset");
  public static final Metric<String> RULESET_VERSION = ofString("rulesetVersion");
  public static final Metric<Long> RULE_EXTRACTION_MILLIS = ofNumber("ruleExtractionMillis");
  public static final Metric<InputSource> INPUT_SOURCE = ofEnum("inputSource");
  public static final Metric<Long> COMPILE_MILLIS = ofNumber("compileMillis");
  public static final Metric<Boolean> CLASSPATH_FALLBACK = ofBoolean("classpathFallback");
  public static final Metric<Long> ANALYSIS_MILLIS = ofNumber("analysisMillis");
  public static final Metric<Integer> TOTAL_ERRORS = ofNumber("totalErrors");
  public static final Metric<Map<RuleKind, Integer>> ERRORS_PER_RULE_KIND =
      ofEnumCounts("errorsPerRuleKind", RuleKind.class);
  public static final Metric<Map<String, Integer>> ERRORS_PER_CRYSL_RULE =
      ofCounts("errorsPerCryslRule");
  public static final Metric<Integer> SEEDS = ofNumber("seeds");
  public static final Metric<Integer> CLASSES_WITH_CRYPTO_USAGE =
      ofNumber("classesWithCryptoUsage");
  public static final Metric<Integer> METHODS_WITH_CRYPTO_USAGE =
      ofNumber("methodsWithCryptoUsage");
  public static final Metric<Outcome> OUTCOME = ofEnum("outcome");
  public static final Metric<Long> TOTAL_MILLIS = ofNumber("totalMillis");

  private MetricDefinitions() {}
}
