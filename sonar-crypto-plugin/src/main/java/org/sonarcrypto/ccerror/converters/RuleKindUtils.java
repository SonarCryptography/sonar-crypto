package org.sonarcrypto.ccerror.converters;

import crysl.rule.CrySLObject;
import crysl.rule.CrySLPredicate;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.RuleKind;

@NullMarked
public class RuleKindUtils {

  public static RuleKind detectRuleKind(CrySLPredicate pred) {
    return detectRuleKind(pred.getPredName());
  }

  public static RuleKind detectRuleKind(CrySLObject obj) {
    final var splitter = obj.getSplitter();

    if (splitter != null) {
      // Source: `ValueConstraint.toString`
      switch (splitter.getIndex()) {
        case 0:
          return RuleKind.ALGORITHM;
        case 1:
          return RuleKind.MODE;
        case 2:
          return RuleKind.PADDING;
      }
    }

    return detectRuleKind(obj.getVarName());
  }

  public static RuleKind detectRuleKind(String value) {
    final var name = value.toLowerCase(Locale.ROOT);

    if (name.contains("alg")) {
      return RuleKind.ALGORITHM;
    }
    if (name.contains("mod")) {
      return RuleKind.MODE;
    }
    if (name.contains("pad")) {
      return RuleKind.PADDING;
    }
    if (name.contains("keylen") || name.contains("length")) {
      return RuleKind.KEY_LENGTH;
    }
    // if(name.contains("keymat")) {
    //  return RuleKind.KEY_MATERIAL;
    // }
    return RuleKind.GENERAL;
  }
}
