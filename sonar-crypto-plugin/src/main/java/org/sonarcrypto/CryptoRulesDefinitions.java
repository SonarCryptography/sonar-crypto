package org.sonarcrypto;

import org.jspecify.annotations.NullMarked;
import org.sonar.api.rule.RuleStatus;
import org.sonarcrypto.cryptorules.CryptoRulesDefinition;
import org.sonarcrypto.cryptorules.Severity;

@NullMarked
public class CryptoRulesDefinitions {

  private CryptoRulesDefinitions() {
    // Private constructor to prevent instantiation
  }

  public static final CryptoRulesDefinition GENERAL =
      CryptoRulesDefinition.builder()
          .withRuleKind(RuleKind.GENERAL)
          .withName("General")
          .withStatus(RuleStatus.READY)
          .withSeverity(Severity.CRITICAL)
          .withDescription(
              """
              <p>Unlike most regular bugs, cryptographic issues don’t announce themselves.
              For example, encryption with
              <a href="https://en.wikipedia.org/wiki/Advanced_Encryption_Standard">AES</a>
              in
              <a href="https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#ECB">ECB</a>
              mode still produces gibberish,
              still round-trips through your unit tests, still passes code review.
              The output looks encrypted, but it just isn't secure.
              That gap between <i>"it works"</i> and <i>"it’s secure"</i> is what makes
              this class of problem different from anything else in your bug tracker.</p>

              <p>In most of your code, a small mistake causes small damage.
              But with cryptography, the failure curve is a cliff.
              Here are some examples of misused cryptography:</p>

              <ul>
                 <li>
                   <em><code>Random</code> or <code>Math.random()</code> for a token:</em>
                   Predictable session IDs, password reset links,
                   or <a href="https://en.wikipedia.org/wiki/Cross-site_request_forgery">CSRF</a>
                   tokens.
                 </li>
                 <li>
                    <em>Lazy algorithm specification:</em>
                    Just specifying, for example, <code>"AES"</code>, results in an AES instance
                    with the <em>unsecure</em> ECB mode.
                 </li>
                 <li>
                   <em>Encrypt w/o authentication:</em>
                   Malleable ciphertext an attacker can tamper with,
                   which has repeatedly turned into full plaintext recovery via
                   <a href="https://en.wikipedia.org/wiki/Padding_oracle_attack">padding oracles</a>.
                 </li>
                 <li>
                   <em>Self-knitted encrypt-with-authentication:</em>
                   Wrong implemented authentication of variable-length messages allows an attacker
                   to shift the boundary between additional authenticated data (AAD)
                   and the ciphertext to produce different messages.
                 </li>
                 <li>
                   <em>Hardcoded key or IV:</em>
                   Usually added “temporarily” to get a test passing,
                   then committed into a version control system, then shipped.
                 </li>
                 <li>
                   <em>Nonce reuse with AES/GCM:</em>
                   Reuse one nonce with one key, and you don’t just leak two plaintexts’ XOR,
                   you leak the authentication key, letting an attacker forge messages.
                 </li>
                 <li>
                   <em>Non-constant-time comparison of a hash or a MAC:</em>
                   Comparing byte arrays using <code>Arrays.equals(…)</code> lets an attacker recover
                   it byte by byte through a
                   <a href="https://en.wikipedia.org/wiki/Timing_attack">timing attack</a>.
                 </li>
              </ul>
              """)
          .withHowToFixSection(
              """
              <p>The practical conclusion is to know the cryptographic APIs you are using,
              allowing you to apply the correct algorithms and utilities in the correct manner.</p>

              <ul>
                 <li>
                   Never implement a primitive, protocol, or padding scheme yourself,
                   however clear the spec might appear.
                 </li>
                 <li>
                   Randomness comes from a
                   <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#secure-random-number-generation">cryptographically secure pseudo-random number generator (CSPRNG)</a>,
                   such as <a href="https://www.baeldung.com/java-secure-random"><code>SecureRandom</code></a>,
                   <em>never</em> from a PRNG.
                 </li>
                 <li>
                   Passwords go through a <a href="https://en.wikipedia.org/wiki/Key_derivation_function">key derivation function</a>
                   (e.g., <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2">PBKDF2</a>
                   or <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#argon2id">Argon2id</a>),
                   <em>never</em> through a general-purpose hash.
                 </li>
                 <li>
                    Always specify the complete cipher transformation consisting of algorithm,
                    mode, and padding, e.g., <code>"AES/GCM/NoPadding"</code>,
                    to avoid unsecure defaults.
                 </li>
                 <li>
                   Always encrypt and authenticate via
                   <a href="https://en.wikipedia.org/wiki/Authenticated_encryption#Authenticated_encryption_with_associated_data">AEAD</a>,
                   such as the <a href="https://en.wikipedia.org/wiki/Galois/Counter_Mode">Galois/Counter Mode (GCM)</a>
                   or via Tink’s <code>Aead</code> primitive.
                 </li>
                 <li>
                    Check recommendations for key length, IV size, cost, or iterations to ensure a secure encryption,
                    hashing, or password derivation.
                 </li>
                 <li>
                   Keys live in a key management system, secret manager,
                   or env config&mdash;<em>not</em> in source code,
                   <em>not</em> in a version control system.
                 </li>
                 <li>
                    Use constant-time comparison of hashes and MACs via
                    <a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/security/MessageDigest.html#isEqual(byte%5B%5D,byte%5B%5D)"><code>MessageDigest.isEqual(…)</code></a>
                    to avoid timing attacks.
                 </li>
              </ul>
              """)
          .withResourcesSection(
              """
              <h3>Oracle</h3>
              <ul>
                  <li><a href="https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html">Java Cryptography Architecture (JCA) Reference Guide</a></li>
                  <li><a href="https://docs.oracle.com/en/java/javase/25/docs/specs/security/standard-names.html">Java Security Standard Algorithm Names</a></li>
              </ul>

              <h3>OWASP</h3>
              <ul>
                <li><a href="https://cheatsheetseries.owasp.org/">OWASP Cheat Sheet Series Project</a></li>
                <ul>
                  <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a></li>
                  <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#secure-random-number-generation">Secure Random Number Generation</a></li>
                  <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">Password Storage Cheat Sheet</a></li>
                </ul>
              </ul>

              <h3>Baeldung</h3>
              <ul>
                <li>
                  <a href="https://www.baeldung.com/category/security">Security in Java applications</a>
                </li>
              </ul>

              <h3>Wikipedia</h3>
              <ul>
                <li><a href="https://en.wikipedia.org/wiki/Authenticated_encryption">Authenticated encryption</a></li>
                <li><a href="https://en.wikipedia.org/wiki/Key_derivation_function">Key derivation function</a></li>
              </ul>
              <ul>
                <li><a href="https://en.wikipedia.org/wiki/Category:Cryptographic_attacks">Cryptographic attacks</a></li>
                <li><a href="https://en.wikipedia.org/wiki/Category:Web_security_exploits">Web security exploits</a></li>
              </ul>
              """)
          .build();

  public static final CryptoRulesDefinition ALGORITHM =
      CryptoRulesDefinition.builder()
          .withRuleKind(RuleKind.ALGORITHM)
          .withName("Unsecure Algorithm")
          .withKey("algorithm")
          .withStatus(RuleStatus.READY)
          .withSeverity(Severity.CRITICAL)
          .withDescription(
              """
              <h3>Broken Hash Algorithms</h3>

              <p>Broken hash algorithms,
              such as <a href="https://en.wikipedia.org/wiki/MD5"> MD5</a>
              or <a href="https://en.wikipedia.org/wiki/SHA-1">SHA-1</a>,
              allow attackers to run
              <a href="https://en.wikipedia.org/wiki/Collision_attack#Chosen-prefix_collision_attack">collision attacks</a>
              by computing the same hashes from different data.
              Hence, one cannot rely on those hashes anymore.</p>

              <h3>Broken Message Authentication</h3>

              <p>Do not build a <a href="https://en.wikipedia.org/wiki/Message_authentication_code">MAC</a>
              from untruncated SHA2, such as SHA-256 or SHA-512,
              since those hashes are vulnerable to the
              <a href="https://en.wikipedia.org/wiki/Length_extension_attack">length extension attack</a>.
              While this vulnerability is not relevant for other applications of the hash function,
              it is a major flaw when used for message authentication,
              as this allows an attacker to extend the MAC to authenticate a modified message
              without needing to know the secret or the original message.</p>

              <p>But that’s not the only pitfall.
              For example, improper MACing of
              <a href="https://en.wikipedia.org/wiki/CBC-MAC#Security_with_fixed_and_variable-length_messages">variable-length messages</a>
              can enable an attacker to manipulate the boundary between additional authenticated
              data (AAD) and the ciphertext to produce different messages.</p>

              <h3>Broken Encryption Algorithms</h3>

              <p>Broken encryption algorithms, such as
              <a href="https://en.wikipedia.org/wiki/Data_Encryption_Standard">DES</a>,
              allow attackers to reverse-compute the secret key through brute-force attacks.</p>

              <h3>Dummy Algorithms</h3>

              <p>The JCA, for example, provides an
              <a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/javax/crypto/NullCipher.html"><em>“identity cipher”</em></a>
              (or <em>“NULL cipher”</em>)
              that does not transform the input at all,
              meaning that the output is the unaltered plain text.</p>
              """)
          .withHowToFixSection(
              """
              <h3>Hashing</h3>

              <p>Use algorithms of the <a href="https://en.wikipedia.org/wiki/SHA-1">SHA-2</a>
              or <a href="https://en.wikipedia.org/wiki/SHA-3">SHA-3</a>
              families, e.g., SHA-256 or SHA3-256,
              where SHA-2 is still preferred.</p>

              <h3>Message Authentication</h3>

              <p>Build a MAC using <a href="https://en.wikipedia.org/wiki/HMAC">HMAC</a>;
              do <em>not</em> use plain SHA-2.</p>

              <p>If you need to build a MAC based on SHA-3, use KMAC.
              Since KMAC is still rarely used, you should prefer HMAC.</p>

              <h3>Encryption</h3>

              <h4>Encrypt with Authentication</h4>

              Always encrypt <em>and</em> authenticate.
              But do not use a custom implementation;
              instead, use <a href="https://en.wikipedia.org/wiki/Authenticated_encryption#Authenticated_encryption_with_associated_data">AEAD</a>,
              such as the <a href="https://en.wikipedia.org/wiki/Galois/Counter_Mode">Galois/Counter Mode (GCM)</a>
              or via Tink’s <code>Aead</code> primitive.
              This ensures that, for example, variable length messages are handled correctly.

              <h4>Symmetric Encryption</h4>

              <p>Use a strong encryption algorithm and mode that includes authentication,
              such as AES in combination with
              <a href="https://de.wikipedia.org/wiki/Galois/Counter_Mode">GCM</a>.</p>

              <p>In general but especially for GCM,
              it is very important to <em>never</em> reuse an initialization vector,
              as this would allow an attacker to recover the secret for the message authentication.</p>

              <h4>Asymmetric Encryption</h4>

              <p>Use <a href="https://de.wikipedia.org/wiki/Elliptic_Curve_Cryptography">elliptical curve cryptography (ECC)</a>
              for asymmetric encryption, where
              <a href="https://en.wikipedia.org/wiki/Curve25519">Curve25519</a> is the
              <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#algorithms">preferred algorithm</a>.</p>

              <p>If you have to use <a href="https://en.wikipedia.org/wiki/RSA_cryptosystem">RSA</a>,
              the key length should be <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#algorithms">at least 2048 bits</a>.</p>

              <p>The Curve25519 ECC algorithm, for example, as a key size of 256 bits
              and offers 128 bits of security
              that is considered <a href="https://www.ssl.com/article/comparing-ecdsa-vs-rsa-a-simple-guide/">as strong as RSA with 3072 bits</a>,
              but requires significantly less calculation effort.</p>
              """)
          .withResourcesSection(
              """
             <h3>Oracle</h3>
              <ul>
                <li><a href="https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html">Java Cryptography Architecture (JCA) Reference Guide</a></li>
                <li><a href="https://docs.oracle.com/en/java/javase/25/docs/specs/security/standard-names.html">Java Security Standard Algorithm Names</a></li>
              </ul>

              <h3>OWASP</h3>
              <ul>
                <li><a href="https://cheatsheetseries.owasp.org/">OWASP Cheat Sheet Series Project</a></li>
                <ul>
                  <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a></li>
                  <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#secure-random-number-generation">Secure Random Number Generation</a></li>
                  <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">Password Storage Cheat Sheet</a></li>
                </ul>
              </ul>

              <h3>Baeldung</h3>
              <ul>
                <li>
                  <a href="https://www.baeldung.com/category/security">Security in Java applications</a>
                </li>
              </ul>

              <h3>Wikipedia</h3>
              <ul>
                <li><a href="https://en.wikipedia.org/wiki/Authenticated_encryption">Authenticated encryption</a></li>
                <li><a href="https://en.wikipedia.org/wiki/Key_derivation_function">Key derivation function</a></li>
              </ul>
              <ul>
                <li><a href="https://en.wikipedia.org/wiki/Category:Cryptographic_hash_functions">Cryptographic hash functions</a></li>
                <li><a href="https://en.wikipedia.org/wiki/Category:Broken_hash_functions">Broken hash functions</a></li>
              </ul>
             """)
          .build();

  public static final CryptoRulesDefinition MODE =
      CryptoRulesDefinition.builder()
          .withRuleKind(RuleKind.MODE)
          .withName("Unsecure Mode")
          .withKey("mode")
          .withStatus(RuleStatus.READY)
          .withSeverity(Severity.CRITICAL)
          .withDescription(
              """
              <h3>Insecure Modes</h3>

              <p>The “mode of operation” is specific to block ciphers, such as
              <a href="https://en.wikipedia.org/wiki/Advanced_Encryption_Standard">AES</a>.
              Using insecure modes allows an attacker, e.g., to discover patterns in the cipher text.
              <a href="https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#ECB">ECB</a>
              is such a mode that counts as insecure if applied for general-purpose encryption,
              because encrypting more than one block of data may introduce discoverable patterns.</p>

              <h3>Broken Modes</h3>

              <p>The
              <a href="https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#Propagating_cipher_block_chaining_(PCBC)">PCBC</a>
              mode, for example, is broken, because it falsely claimed integrity.
              Code that relies on this claim might be tricked by manipulating the cipher text,
              which may case vulnerabilities.</p>

              <h3>Default Mode</h3>

              <p>For example, when specifying the AES algorithm without a mode
              (<code>Cipher.getInstance("AES")</code>),
              the JCA returns a cipher instance with the
              <a href="https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#ECB">ECB</a>
              mode that is unsecure if applied for general-purpose encryption.</p>
              """)
          .withHowToFixSection(
              """
              <h3>Secure and Recommended Modes</h3>
              
              Use <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#cipher-modes">secure and recommended modes</a>.
              Check regularly, whether the state of the used modes changes.
              
              <h3>Never Rely on Defaults</h3>

              <p>Always specify the complete cipher transformation consisting of algorithm, mode,
              and padding, e.g. <code>"AES/GCM/NoPadding"</code>.</p>

              <h3>Use Mode with Authentication</h3>

              <p>Use a mode that provides authentication, such as
              <a href="https://en.wikipedia.org/wiki/Galois/Counter_Mode">GCM</a>.</p>
              """)
          .withResourcesSection(
              """
              <h3>Oracle</h3>
              <ul>
                <li><a href="https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html">Java Cryptography Architecture (JCA) Reference Guide</a></li>
                <li><a href="https://docs.oracle.com/en/java/javase/25/docs/specs/security/standard-names.html#cipher-algorithm-modes">Cipher Algorithm Modes</a></li>
              </ul>

              <h3>OWASP</h3>
              <ul>
                <li>
                  <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a>
                </li>
              </ul>
             """)
          .build();

  public static final CryptoRulesDefinition PADDING =
      CryptoRulesDefinition.builder()
          .withRuleKind(RuleKind.PADDING)
          .withName("Unsecure Padding")
          .withKey("padding")
          .withStatus(RuleStatus.READY)
          .withSeverity(Severity.CRITICAL)
          .withDescription(
              """
              <h3>Broken Padding</h3>
              
              <p>For example, in combination with RSA, <code>NoPadding</code> generates deterministic
              output and <code>PKCS1Padding</code> is vulnerable to
              <a href="https://en.wikipedia.org/wiki/Adaptive_chosen-ciphertext_attack">Bleichenbacher’s attack</a>.</p>
              
              <h3>Default Padding</h3>
              
              <p>For example, when specifying the RSA algorithm without a padding
              (<code>Cipher.getInstance("RSA")</code>),
              the JCA returns a cipher instance with the broken <code>PKCS1Padding</code> scheme.</p>
              """)
          .withHowToFixSection(
              """
              <h3>Secure and Recommended Padding Schemes</h3>
              
              <p>Use <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#random-padding">secure and recommended padding schemes</a>.
              Check regularly, whether the state of the used padding schemes changes.</p>
              
              <h3>Never Rely on Defaults</h3>

              <p>Always specify the complete cipher transformation consisting of algorithm, mode,
              and padding, e.g. <code>"RSA/ECB/OAEPWithSHA-256AndMGF1Padding"</code>,
              where <code>"ECB"</code> is just a placeholder,
              because the syntax expects three parts in the cipher transformation;
              meaning, RSA has no mode of operation.</p>

              <h3>Use Mode with Authentication</h3>

              <p>Use a mode that provides authentication, such as
              <a href="https://en.wikipedia.org/wiki/Galois/Counter_Mode">GCM</a>.</p>
              """)
          .withResourcesSection(
              """
              <h3>Oracle</h3>
              <ul>
                <li><a href="https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html">Java Cryptography Architecture (JCA) Reference Guide</a></li>
                <li><a href="https://docs.oracle.com/en/java/javase/25/docs/specs/security/standard-names.html#cipher-algorithm-paddings">Cipher Algorithm Paddings</a></li>
              </ul>

              <h3>OWASP</h3>
              <ul>
                <li>
                  <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a>
                </li>
              </ul>
             """)
          .build();

  public static final CryptoRulesDefinition KEY_MATERIAL =
      CryptoRulesDefinition.builder()
          .withRuleKind(RuleKind.KEY_MATERIAL)
          .withName("Unsecure Key Material")
          .withKey("key material")
          .withStatus(RuleStatus.READY)
          .withSeverity(Severity.CRITICAL)
          .withDescription(
              """
              <h3>Improper Generated Key</h3>
              
              <p>Keys with low entropy or insufficient randomness
              can be easily guessed by an attacker.</p>

              <h3>Small Keys</h3>
              
              <p>Keys with an insufficient key length may allow an attacker to break the encryption.</p>

              <h3>Hardcoded Keys</h3>
              
              <p>Keys that are hardcoded, are present in the source code and in the compiled binary
              and thus discoverable by an attacker.</p>
              """)
          .withHowToFixSection(
              """
              <h3>Generate Keys Properly</h3>
              
              <p>Choose an appropriate method to generate the key.
              Use a
              <a href="https://en.wikipedia.org/wiki/Cryptographically_secure_pseudorandom_number_generator">cryptographically secure pseudorandom number generator (CSPRNG)</a>
              (<a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/security/SecureRandom.html"><code>java.util.SecureRandom</code></a>)
              to generate key material that is based on randomness.</p>

              <h3>Appropriate Key Length</h3>

              <p>The length of keys heavily depends on the algorithm used;
              here are some examples for appropriate key lengths:</p>

              <table>
              <tr><th>Algorithm</th><th>Recommended Key Length</th></tr>
              <tr><td><a href="https://en.wikipedia.org/wiki/Advanced_Encryption_Standard">AES</a></td><td>at least 128 bits, ideally 256 bits</td></tr>
              <tr><td><a href="https://en.wikipedia.org/wiki/RSA_cryptosystem">RSA</a></td><td>at least 2048 bits, ideally 3096 bits or higher</td></tr>
              <tr><td><a href="https://en.wikipedia.org/wiki/Elliptic-curve_cryptography">ECC</a> with <a href="https://en.wikipedia.org/wiki/Curve25519">Curve25519</a></td><td>256 bits (effectively 128 bits); corresponds to approximately 3096 bits of RSA</td></tr>
              <tr><td><a href="https://en.wikipedia.org/wiki/Key_derivation_function">Key derivation</a></td><td>depends on the size of the underlying hash, e.g., 256 bits for <code>PBKDF2WithHmacSHA256</code> or 512 bits for <code>PBKDF2WithHmacSHA384</code> and <code>PBKDF2WithHmacSHA512</code></td></tr>
              </table>

              <h3>Never Hardcode Key Material</h3>
              
              <p>For key material, never use constant values or strings in the source code
              or any other form of resources that is committed into the code repository.</p>
              """)
          .withResourcesSection(
              """
              <h3>Oracle</h3>
              <ul>
                  <li><a href="https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html">Java Cryptography Architecture (JCA) Reference Guide</a></li>
              </ul>

              <h3>OWASP</h3>
              <ul>
                <li><a href="https://cheatsheetseries.owasp.org/">OWASP Cheat Sheet Series Project</a></li>
                <ul>
                    <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a></li>
                    <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#secure-random-number-generation">Secure Random Number Generation</a></li>
                  </ul>
              </ul>
              """)
          .build();

  public static final CryptoRulesDefinition FORBIDDEN_METHOD =
      CryptoRulesDefinition.builder()
          .withRuleKind(RuleKind.FORBIDDEN_METHOD)
          .withName("Forbidden Method")
          .withKey("method")
          .withStatus(RuleStatus.READY)
          .withSeverity(Severity.CRITICAL)
          .withDescription(
              """
              <h3>Cryptographically Unsecure Method</h3>
              <p>A call to a cryptographically unsecure method may result a weak encryption.</p>

              <h3>Non-constant Time Computation</h3>
              <p>Using functions that compute in non-constant time, such as `Object.equals`,
              allow an attacker to gain information about the encryption, e.g.,
              how many digits they guessed correctly.</p>
              """)
          .withHowToFixSection(
              """
              <h3>Cryptographically Secure Method</h3>
              <p>Use a cryptographically secure method for the operation.</p>

              <h3>Constant-Time Computation</h3>
              <p>Use methods that compute values in a constant time,
              such as `MessageDigest.isEqual` to compare two byte arrays.</p>
              """)
          .withResourcesSection(
              """
              <ul>
                <li><a href="https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html">Java Cryptography Architecture (JCA) Reference Guide</a>
                  <ul>
                      <li><a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/security/MessageDigest.html#isEqual(byte%5B%5D,byte%5B%5D)"><code>MessageDigest.isEqual</code></a></a></li>
                  </ul>
                </li>
                <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html#secure-random-number-generation">Secure Random Number Generation</a></li>
              </ul>
              """)
          .build();

  public static final CryptoRulesDefinition UNCAUGHT_EXCEPTION =
      CryptoRulesDefinition.builder()
          .withRuleKind(RuleKind.UNCAUGHT_EXCEPTION)
          .withName("Uncaught Exception")
          .withKey("exception")
          .withStatus(RuleStatus.READY)
          .withSeverity(Severity.CRITICAL)
          .withDescription(
              """
              <h3>Uncaught Exception</h3>

              <p>Exceptions concerning cryptography, e.g., an invalid padding,
              may contain sensitive information that might be useful for the attacker
              to break the encryption.</p>
              """)
          .withHowToFixSection(
              """
              <p>Do not expose details about cryptographic errors.</p>
              """)
          .withResourcesSection(
              """
              <ul>
                <li><a href="https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html">Java Cryptography Architecture (JCA) Reference Guide</a></li>
                <li><a href="https://en.wikipedia.org/wiki/Padding_oracle_attack">Padding oracle attack</a></li>
              </ul>
              """)
          .build();

  public static final CryptoRulesDefinition API_MISUSE =
      CryptoRulesDefinition.builder()
          .withRuleKind(RuleKind.API_MISUSE)
          .withName("API Misuse")
          .withKey("misuse")
          .withStatus(RuleStatus.READY)
          .withSeverity(Severity.CRITICAL)
          .withDescription(
              """
              <h3>API Misuse</h3>
              <p>To gain a secure cryptography, it is mandatory to use the crypto API correctly.
              A misuse may result in poorly encrypted data
              that an attacker might beable to decrypt.</p>
              """)
          .withHowToFixSection(
              """
              <p>Use cryptographic APIs exactly as intended.
              It is therefore recommended to read the documentation thorougly.</p>
              """)
          .withResourcesSection(
              """
              <ul>
                <li><a href="https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html">Java Cryptography Architecture (JCA) Reference Guide</a></li>
                <li><a href="https://cheatsheetseries.owasp.org/index.html">OWASP Cheat Sheet Series</a>
                  <ul>
                    <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html">Cryptographic Storage Cheat Sheet</a></li>
                    <li><a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">Password Storage Cheat Sheet</a></li>
                  </ul>
                </li>
              </ul>
              """)
          .build();

  public static CryptoRulesDefinition fromRuleKind(RuleKind ruleKind) {
    return switch (ruleKind) {
      case GENERAL -> CryptoRulesDefinitions.GENERAL;
      case ALGORITHM -> CryptoRulesDefinitions.ALGORITHM;
      case MODE -> CryptoRulesDefinitions.MODE;
      case PADDING -> CryptoRulesDefinitions.PADDING;
      case KEY_MATERIAL -> CryptoRulesDefinitions.KEY_MATERIAL;
      case FORBIDDEN_METHOD -> CryptoRulesDefinitions.FORBIDDEN_METHOD;
      case UNCAUGHT_EXCEPTION -> CryptoRulesDefinitions.UNCAUGHT_EXCEPTION;
      case API_MISUSE -> CryptoRulesDefinitions.API_MISUSE;
    };
  }
}
