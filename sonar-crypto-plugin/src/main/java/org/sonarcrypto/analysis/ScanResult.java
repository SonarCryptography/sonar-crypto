package org.sonarcrypto.analysis;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.ccerror.ConvertedError;

/** Result of a CogniCrypt scan: the converted errors plus aggregated analysis info. */
@NullMarked
public record ScanResult(List<ConvertedError> errors, CryptoAnalysisInfo info) {}
