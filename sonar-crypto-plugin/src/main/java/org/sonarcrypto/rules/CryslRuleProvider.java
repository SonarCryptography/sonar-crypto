package org.sonarcrypto.rules;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a method to download and extract CrySL rules from the CROSSING repository.
 * The repo currently contains rules for three Crypto Libraries :
 * BouncyCastle, BouncyCastle-JCA, and JavaCryptographicArchitecture.
 * The rulesets can not be used at the same time due to conflicting file names.
 * The rules are extracted to a temporary directory.
 */
public class CryslRuleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CryslRuleProvider.class);
    private final URL ruleDistribution;

    public CryslRuleProvider() {
        try {
            this.ruleDistribution = new URL("https://github.com/CROSSINGTUD/Crypto-API-Rules/archive/refs/heads/master.zip");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Downloads the zip from ruleDistribution, extracts all .crysl files to a temp directory, and returns the directory.
     * @return The temp directory containing the extracted .crysl files.
     * @throws IOException if an I/O error occurs
     */
    public Path extractCryslFilesToTempDir(Predicate<String> filter) throws IOException {
        Path tempDir = Files.createTempDirectory("crysl_rules");
        java.net.URLConnection connection = ruleDistribution.openConnection();
        int count = 0;
        try (java.io.InputStream is = connection.getInputStream();
             java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is)) {
            java.util.zip.ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".crysl") && filter.test(entry.getName())) {
                    Path outFile = tempDir.resolve(Paths.get(entry.getName()).getFileName());
                    Files.write(outFile, zis.readAllBytes());
                    count++;
                }
                zis.closeEntry();
            }
        }
        LOGGER.info("Extracted {} CrySL files to {}", count, tempDir.toAbsolutePath());
        return tempDir;
    }
}
