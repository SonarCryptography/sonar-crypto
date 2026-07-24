package org.sonarcrypto.ccerror;

import boomerang.scope.Method;
import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.ccerror.violations.Violation;
import org.sonarcrypto.utils.sonar.FqClassName;
import sootup.core.jimple.basic.SimpleStmtPositionInfo;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.model.Position;

@NullMarked
public record ConvertedError(
    FqClassName className, StmtPositionInfo position, Method method, Violation violation) {
  public ConvertedError(
      FqClassName className, Position position, Method method, Violation violation) {
    this(className, new SimpleStmtPositionInfo(position), method, violation);
  }
}
