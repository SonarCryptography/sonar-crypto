package org.sonarcrypto;

import boomerang.scope.Method;
import boomerang.scope.WrappedClass;
import com.google.common.collect.Table;
import crypto.analysis.errors.AbstractError;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import org.sonarcrypto.rules.CryslRuleProvider;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class CogniCryptTestRunner {

    Table<WrappedClass, Method, Set<AbstractError>> errorCollection;

    public void run(String applicationPath, RuleSet rules) throws IOException {
        var provider = new CryslRuleProvider();
        var scanner = new HeadlessJavaScanner(applicationPath, Objects.requireNonNull(provider.extractCryslFileToTempDir(rules.dirName)).toString());
        scanner.scan();
        errorCollection = scanner.getCollectedErrors();
    }

    public Table<WrappedClass, Method, Set<AbstractError>> getErrorCollection() {
        return errorCollection;
    }
}
