package org.sonarcrypto.utility.groundtruth;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.RuleKind;
import org.sonarcrypto.ccerror.causes.Cause;

@NullMarked
public record GroundTruthEntry(
    RuleKind ruleKind, Class<? extends Cause> causeType, @Nullable String value) {}
