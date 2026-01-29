package org.sonarcrypto;

import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import org.sonarcrypto.rules.CryslRuleProvider;

import java.io.IOException;
import java.util.Objects;

public class CogniCryptTestRunner {

    public void run(String applicationPath, RuleSet rules) throws IOException {
        CryslRuleProvider provider = new CryslRuleProvider();
        HeadlessJavaScanner scanner = new HeadlessJavaScanner(applicationPath, Objects.requireNonNull(provider.extractCryslFileToTempDir(rules.dirName)).toString());
    }
}
