package org.sonarcrypto.utils.cognicrypt;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.sonarcrypto.utils.jimple.mapper.LineMapping;
import sootup.core.jimple.basic.SimpleStmtPositionInfo;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;
import sootup.core.jimple.visitor.StmtVisitor;
import sootup.core.model.Body;
import sootup.core.transform.BodyInterceptor;
import sootup.core.views.View;

public class LocationReplacerInterceptor implements BodyInterceptor {

  private final Map<Integer, LineMapping> statementMappings;

  public LocationReplacerInterceptor(Map<Integer, LineMapping> statementMappings) {
    this.statementMappings = statementMappings;
  }

  @Override
  public void interceptBody(Body.BodyBuilder builder, @NonNull View view) {
    Map<Stmt, Stmt> stmtsToReplace = new LinkedHashMap<>();
    builder
        .getStmtGraph()
        .iterator()
        .forEachRemaining(
            stmt -> {
              LineMapping mapping =
                  statementMappings.get(stmt.getPositionInfo().getStmtPosition().getFirstLine());
              if (mapping != null) {
                StmtPositionInfo newPosInfo =
                    new SimpleStmtPositionInfo(mapping.getSourcePosition().toSootUpPosition());
                PositionReplacer replacer = new PositionReplacer(newPosInfo);
                stmt.accept(replacer);
                if (replacer.result != null && replacer.result != stmt) {
                  stmtsToReplace.put(stmt, replacer.result);
                }
              }
            });
    stmtsToReplace.forEach((old, updated) -> builder.getStmtGraph().replaceNode(old, updated));
  }

  private static class PositionReplacer implements StmtVisitor {

    private final StmtPositionInfo newPositionInfo;
    Stmt result = null;

    private PositionReplacer(StmtPositionInfo newPositionInfo) {
      this.newPositionInfo = newPositionInfo;
    }

    @Override
    public void caseBreakpointStmt(JBreakpointStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseInvokeStmt(JInvokeStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseAssignStmt(JAssignStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseIdentityStmt(JIdentityStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseEnterMonitorStmt(JEnterMonitorStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseExitMonitorStmt(JExitMonitorStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseGotoStmt(JGotoStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseIfStmt(JIfStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseNopStmt(JNopStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseRetStmt(JRetStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseReturnStmt(JReturnStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseReturnVoidStmt(JReturnVoidStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseSwitchStmt(JSwitchStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void caseThrowStmt(JThrowStmt stmt) {
      result = stmt.withPositionInfo(newPositionInfo);
    }

    @Override
    public void defaultCaseStmt(Stmt stmt) {
      result = stmt; // unknown type — leave unchanged
    }
  }
}
