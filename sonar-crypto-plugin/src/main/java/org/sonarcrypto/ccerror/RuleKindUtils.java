package org.sonarcrypto.ccerror;

import crysl.rule.CrySLObject;
import crysl.rule.CrySLPredicate;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.RuleKind;

@NullMarked
public class RuleKindUtils {
  private static final Pattern KEY_MATERIAL_PATTERN = Pattern.compile("(?>key|length|rand|seed)");

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
    if (KEY_MATERIAL_PATTERN.matcher(name).find()) {
      return RuleKind.KEY_MATERIAL;
    }
    return RuleKind.GENERAL;
  }
}
