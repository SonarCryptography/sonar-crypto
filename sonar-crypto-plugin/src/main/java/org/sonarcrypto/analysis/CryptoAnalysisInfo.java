package org.sonarcrypto.analysis;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.RuleKind;

/** Aggregated information about a single CogniCrypt analysis run. */
@NullMarked
public record CryptoAnalysisInfo(
    InputSource inputSource,
    long compileMillis,
    long analysisMillis,
    int totalErrors,
    Map<RuleKind, Integer> errorsPerRuleKind,
    int classesAnalyzed,
    int methodsAnalyzed) {}
