package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;

@NullMarked
public class CryptoRulesDefinitions {
  public static final CryptoRulesDefinition CC1 =
      CryptoRulesDefinition.builder()
          .withRule("CC1")
          .withName("Cryptographic API Misuse")
          .withDescription(
              "Detects misuses of cryptographic APIs that could lead to security vulnerabilities.")
          .withStatus(RuleStatus.BETA)
          .withSeverity(org.sonar.api.rule.Severity.CRITICAL)
          .withHowToFixSection("<p>How to fix ...</p>")
          .withAssessSection("<p>Assess the problem ...</p>")
          .withResourcesSection(
              "<ul><li><a href=\"https://cheatsheetseries.owasp.org/\">OWASP Cheat Sheet Series Project</a></li></ul>")
          .build();
}
