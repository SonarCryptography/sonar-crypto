package org.sonarcrypto.utils.cognicrypt.crysl;

import static org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils.*;

import boomerang.scope.Statement;
import crypto.extractparameter.ParameterWithExtractedValues;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

@NullMarked
public record CallInfo(@Nullable CalleeInfo calleeInfo, int argumentIndex) {

  public void createMessage(@Nullable String key, StringBuilder messageBuilder) {
    messageBuilder.append(
        String.format(
            "The %s, given as %s to %s, ",
            key != null ? key : "value",
            stringifyArgumentIndex(
                argumentIndex, calleeInfo != null ? calleeInfo.argumentCount() : -1),
            stringifyCallee(calleeInfo)));
  }

  public static CallInfo none() {
    return new CallInfo(null, -1);
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

  public static void createMessage(
      @Nullable CallInfo callInfo, @Nullable String key, StringBuilder messageBuilder) {
    if (callInfo != null) {
      callInfo.createMessage(key, messageBuilder);
    } else {
      messageBuilder
          .append("The ")
          .append(key != null ? key : "value")
          .append(", given as argument, ");
    }
  }
}
