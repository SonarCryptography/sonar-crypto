package org.sonarcrypto.cryptorules;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum Severity {
  INFO,
  MINOR,
  MAJOR,
  CRITICAL,
  BLOCKER;

  /** Converts this severity to {@link org.sonar.api.issue.impact.Severity}. */
  org.sonar.api.issue.impact.Severity toImpactSeverity() {
    return switch (this) {
      case BLOCKER -> org.sonar.api.issue.impact.Severity.BLOCKER;
      case CRITICAL -> org.sonar.api.issue.impact.Severity.HIGH;
      case MAJOR -> org.sonar.api.issue.impact.Severity.MEDIUM;
      case MINOR -> org.sonar.api.issue.impact.Severity.LOW;
      case INFO -> org.sonar.api.issue.impact.Severity.INFO;
    };
  }

  /** Converts this severity to {@link org.sonar.api.rule.Severity} representatives. */
  String toRuleSeverity() {
    return switch (this) {
      case BLOCKER -> org.sonar.api.rule.Severity.BLOCKER;
      case CRITICAL -> org.sonar.api.rule.Severity.CRITICAL;
      case MAJOR -> org.sonar.api.rule.Severity.MAJOR;
      case MINOR -> org.sonar.api.rule.Severity.MINOR;
      case INFO -> org.sonar.api.rule.Severity.INFO;
    };
  }
}
