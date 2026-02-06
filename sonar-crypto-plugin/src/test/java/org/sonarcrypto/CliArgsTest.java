package org.sonarcrypto;

import static org.assertj.core.api.Assertions.assertThat;

import de.fraunhofer.iem.scanner.ScannerSettings.Framework;
import org.junit.jupiter.api.Test;

public class CliArgsTest {

  @Test
  void parseTest() {
    String[] args = {"-cp=crysl_rules", "--ruleset=jca", "-f=soot", "-mvn=MavenProject"};
    var cliArgs = CliArgs.parse(args);

    assertThat(cliArgs).isNotNull();
  }

  @Test
  void getterTest() {
    String[] args = {"-cp=crysl_rules", "--ruleset=jca", "-f=soot", "-mvn=MavenProject"};
    var cliArgs = CliArgs.parse(args);

    assertThat(cliArgs.getClassPath()).isEqualTo("crysl_rules");
    assertThat(cliArgs.getFramework()).isEqualTo(Framework.SOOT);
    assertThat(cliArgs.getRuleset()).isEqualTo("jca");
    assertThat(cliArgs.getMvnProject()).isEqualTo("MavenProject");
  }

  @Test
  void callTest() {
    String[] args = {"-cp=crysl_rules", "--ruleset=jca", "-f=soot", "-mvn=MavenProject"};
    var cliArgs = CliArgs.parse(args);

    assertThat(cliArgs.call()).isZero();
  }
}
