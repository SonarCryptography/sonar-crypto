package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.cryptorules.Severity;

@NullMarked
public class CryptoRulesDefinitions {
  public static final CryptoRulesDefinition CC1 =
      CryptoRulesDefinition.builder()
          .withRule("CC1")
          .withName("Unsecure Algorithm")
          .withDescription(
              "The algorithm or mode specified is cryptographically unsecure, "
                  + "either because it is considered as too weak, like SHA1, "
                  + "or it is cryptographically broken, like MD5.")
          .withStatus(RuleStatus.BETA)
          .withSeverity(Severity.CRITICAL)
          .withAssessSection("<p>Assess the problem ...</p>")
          .withHowToFixSection(
              "<p>This issue can be fixed by choosing a cryptographical secure algorithm or mode.</p>"
                  + "<h2>Hashing</h2>"
                  + "<p>Instead of MD5 or SHA1, use as algorithms of the SHA-2 or SHA-3 families, e.g., SHA-256 or SHA3-256.</p>"
                  + "<p>Beware that untrimmed SHA-2 modes are, despite considered as secure, vulnerable to length extension attacks. "
                  + "Consider using a trimmed mode instead, like SHA-512/256, or use SHA-3 that is not vulnerable to this kind of attack."
                  + "<h2>Encryption</h2>"
                  + "<p>Instead of AES with ECB or CBC mode, use AES in combination with GCM mode.</p>"
                  + "<p>Instead of DSA, use RSA or ECC.</p>")
          .withResourcesSection(
              "<ul>"
                  + "<li>"
                  + "<a href=\"https://cheatsheetseries.owasp.org/\">OWASP Cheat Sheet Series Project</a>"
                  + "</li>"
                  + "<li>"
                  + "<a href=\"https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html\">Cryptographic Storage Cheat Sheet</a>"
                  + "</li>"
                  + "<li>"
                  + "<a href=\"https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html\">Password Storage Cheat Sheet</a>"
                  + "</li>"
                  + "<li>"
                  + "<a href=\"https://en.wikipedia.org/wiki/Length_extension_attack\">Length extension attack</a>"
                  + "</li>"
                  + "</ul>")
          .build();
}
