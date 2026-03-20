package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.cryptorules.Severity;

@NullMarked
public class CryptoRulesDefinitions {
  public static final CryptoRulesDefinition CC1_GENERAL =
      CryptoRulesDefinition.builder()
          .withRule("CC1")
          .withName("General")
          .withDescription(
              """
              <p>The algorithm or mode specified is cryptographically unsecure,
              either because it is considered as too weak, like SHA1,
              or it is cryptographically broken, like MD5 or DES.</p>
              """)
          .withStatus(RuleStatus.BETA)
          .withSeverity(Severity.CRITICAL)
          .withAssessSection(
              """
              <p>Cryptographic issues are mostly a serious problem,
              as they allow attackers to break the encryption,
              e.g., to disclose the information or to sign data in your name.</p>
              """)
          .withHowToFixSection(
              """
              <p>Check the algorithms you chose, the corresponding mode, padding, key length, cost,
              or iterations to ensure a secure encryption, hashing, or password derivation.</p>
              <p>Check whether you use a
              cryptographically secure pseudo-random number generator (CSPRNG)
              to generate sensitive values, such as initialization vectors.</p>
              <p>Fore storing passwords, use a key derivation function,
              such as PBKDF2 or Argon2id.</p>
              """)
          .withResourcesSection(
              """
              <ul>
              <li><a href="https://cheatsheetseries.owasp.org/">OWASP Cheat Sheet Series Project</a></li>
              <ul>
              <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a></li>
              <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#secure-random-number-generation">Secure Random Number Generation</a></li>
              <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">Password Storage Cheat Sheet</a></li>
              </ul>
              <li><a href="https://en.wikipedia.org/wiki/Length_extension_attack">Length extension attack</a></li>
              </ul>
              """)
          .build();

  public static final CryptoRulesDefinition CC2_ALGORITHM =
      CryptoRulesDefinition.builder()
          .withRule("CC2")
          .withName("Unsecure Algorithm")
          .withDescription(
              """
              <p>The algorithm or mode specified is cryptographically unsecure,
              either because it is considered as too weak, like SHA1,
              or it is cryptographically broken, like MD5 or DES.</p>
              """)
          .withKey("algorithm")
          .withStatus(RuleStatus.BETA)
          .withSeverity(Severity.CRITICAL)
          .withAssessSection(
              """
              <p>Cryptographic issues are mostly a serious problem,
              as they allow attackers to break the encryption,
              e.g., to disclose the information or to sign data in your name.</p>

              <h2>Broken Hash Algorithms</h2>
              <p>Broken hash algorithms, such as MD5, allow attackers to run collision attacks by
              computing same hashes from different data.
              Hence, you cannot rely on those hashes anymore.</p>

              <h2>Broken Encryption Algorithms</h2>
              <p>Broken encryption algorithms, such DES, allow attackers to decrypt the content
              through brute-force attacks and to reverse-compute the secret key.</p>

              <h2>Broken or Weak Encryption Modes</h2>
              <p>The ECB mode of AES, for example, if used for encrypting multiple blocks,
              allows attackers to infer the secret key
              by finding patterns in the encrypted data.</p>
              """)
          .withHowToFixSection(
              """
              <p>This issue can be fixed by choosing a cryptographical secure algorithm or mode.</p>

              <h2>Hashing</h2>
              <p>Instead of MD5 or SHA1, use algorithms of the SHA-2 or SHA-3 families, e.g., SHA-256 or SHA3-256.</p>
              <p>Beware that untrimmed SHA-2 modes are, despite considered as secure, vulnerable to length extension attacks.
              Consider using a trimmed mode instead, like SHA-512/256, or use SHA-3 that is not vulnerable to this kind of attack.</p>

              <h2>Encryption</h2>
              <p>Instead of AES with ECB or CBC mode, use AES in combination with GCM mode.
              In general but especially for GCM, it is very important to never reuse an initialization vector.</p>
              <p>Instead of DSA, use RSA or ECC.</p>
              <p>Consider using post-quantum algorithms. But do not use them alone; always combine them with approved sate-of-the-art algorithms.</p>
              """)
          .withResourcesSection(
              """
              <ul>
              <li><a href="https://cheatsheetseries.owasp.org/">OWASP Cheat Sheet Series Project</a></li>
              <ul>
              <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a></li>
              <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#secure-random-number-generation">Secure Random Number Generation</a></li>
              <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">Password Storage Cheat Sheet</a></li>
              </ul>
              <li><a href="https://en.wikipedia.org/wiki/Length_extension_attack">Length extension attack</a></li>
              </ul>
              """)
          .build();

  // public static final CryptoRulesDefinition CC3_MODE = null;
  // public static final CryptoRulesDefinition CC4_PADDING = null;

  public static final CryptoRulesDefinition CC5_KEY_LEN =
      CryptoRulesDefinition.builder()
          .withRule("CC3")
          .withName("Insufficient Key Length")
          .withDescription(
              """
              <p>The key length specified is insufficient.</p>
              """)
          .withKey("key length")
          .withStatus(RuleStatus.BETA)
          .withSeverity(Severity.CRITICAL)
          .withAssessSection(
              """
              <p>Cryptographic issues are mostly a serious problem,
              as they allow attackers to break the encryption,
              e.g., to disclose the information or to sign data in your name.</p>

              <h2>Insufficient Key Length</h2>
              <p>Cryptography not only relies on secure algorithms,
              but also on the size of the used keys,
              where a key might be a password or a random value,
              e.g., used for an initialization vector.</p>
              """)
          .withHowToFixSection(
              """
              <p>This issue can be fixed by choosing an appropriate key length.
              The key size heavily depends on the algorithm used.</p>

              <h2>Examples</h2>

              <table>
              <tr><th>Algorithm</th><th>Recommended Key Length</th></tr>
              <tr><td>AES</td><td>at least 128 bits, ideally 256 bits</td></tr>
              <tr><td>RSA</td><td>at least 2048 bits, ideally 3096 bits or higher</td></tr>
              <tr><td>Curve25519 (ECC)</td><td>256 bits (but effectively 128 bits); corresponds to approximately 3096 bits of RSA</td></tr>
              </table>
              """)
          .withResourcesSection(
              """
              <ul>
              <li><a href="https://cheatsheetseries.owasp.org/">OWASP Cheat Sheet Series Project</a></li>
              <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a></li>
              <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#secure-random-number-generation">Secure Random Number Generation</a></li>
              </ul>
              """)
          .build();
}
