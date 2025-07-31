package org.sonarcrypto;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.sonarcrypto.cognicrypt.MavenProject;
import org.sonarcrypto.rules.CryslRuleProvider;
import de.fraunhofer.iem.scanner.HeadlessJavaScanner;
import de.fraunhofer.iem.scanner.ScannerSettings;


public class Main {
    public static void main(String[] args) throws IOException {
        // TODO: This is for testing purposes, make configurable
        CryslRuleProvider ruleProvider = new CryslRuleProvider();
        Path ruleDir = ruleProvider.extractCryslFilesToTempDir(s -> s.contains("JavaCryptographicArchitecture/"));
        String mavenProjectPath =
                    new File(args[0])
                            .getAbsolutePath();
        MavenProject mi = new MavenProject(mavenProjectPath);
        mi.compile();
        System.out.println("Built project to directory: " + mi.getBuildDirectory());
        HeadlessJavaScanner scanner = new HeadlessJavaScanner(mi.getBuildDirectory(), ruleDir.toString());
        
        scanner.setFramework(ScannerSettings.Framework.SOOT_UP);
        scanner.scan();
        var errors = scanner.getCollectedErrors();
        System.out.println("Errors: " + errors.size());
    }
}