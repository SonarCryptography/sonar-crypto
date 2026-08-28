package org.sonarcrypto.utils.cognicrypt.crysl;

import boomerang.scope.Statement;
import crypto.extractparameter.ParameterWithExtractedValues;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

@NullMarked
public record CallInfo(@Nullable CalleeInfo calleeInfo, int argumentIndex) {
  public static CallInfo none() {
    return new CallInfo(null, 0);
  }

  public static CallInfo of(ParameterWithExtractedValues param) {
    return of(CalleeInfo.of(param.statement()), param.index());
  }

  public static CallInfo of(@Nullable Statement statement, int argumentIndex) {
    return of(CalleeInfo.of(statement), argumentIndex);
  }

  public static CallInfo of(@Nullable CalleeInfo calleeInfo, int argumentIndex) {
    return new CallInfo(calleeInfo, argumentIndex);
  }
}
