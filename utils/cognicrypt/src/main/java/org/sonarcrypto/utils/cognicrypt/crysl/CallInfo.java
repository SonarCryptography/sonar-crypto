package org.sonarcrypto.utils.cognicrypt.crysl;

import static org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils.*;

import crypto.extractparameter.ParameterWithExtractedValues;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sonarcrypto.utils.cognicrypt.boomerang.CalleeInfo;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@NullMarked
public record CallInfo(Optional<CalleeInfo> calleeInfo, int argumentIndex) {
  public CallInfo(@Nullable CalleeInfo calleeInfo, int argumentIndex) {
    this(Optional.ofNullable(calleeInfo), argumentIndex);
  }

  public void createMessage(@Nullable String key, StringBuilder messageBuilder) {
    messageBuilder.append(
        String.format(
            "The %s, given as %s to %s, ",
            key != null ? key : "value",
            stringifyArgumentIndex(argumentIndex, calleeInfo.map(CalleeInfo::argumentCount)),
            stringifyCallee(calleeInfo)));
  }

  public static Optional<CallInfo> optOf(ParameterWithExtractedValues param) {
    return Optional.of(of(param));
  }

  public static CallInfo of(ParameterWithExtractedValues param) {
    return of(CalleeInfo.of(param.statement()), param.index());
  }

  public static Optional<CallInfo> optOf(@Nullable CalleeInfo calleeInfo, int argumentIndex) {
    return Optional.of(of(calleeInfo, argumentIndex));
  }

  public static Optional<CallInfo> optOf(Optional<CalleeInfo> calleeInfo, int argumentIndex) {
    return Optional.of(of(calleeInfo, argumentIndex));
  }

  public static CallInfo of(@Nullable CalleeInfo calleeInfo, int argumentIndex) {
    return new CallInfo(calleeInfo, argumentIndex);
  }

  public static CallInfo of(Optional<CalleeInfo> calleeInfo, int argumentIndex) {
    return new CallInfo(calleeInfo, argumentIndex);
  }

  public static void createMessage(
      Optional<CallInfo> callInfo, @Nullable String key, StringBuilder messageBuilder) {
    callInfo.ifPresentOrElse(
        it -> it.createMessage(key, messageBuilder),
        () ->
            messageBuilder
                .append("The ")
                .append(key != null ? key : "value")
                .append(", given as argument, "));
  }
}
