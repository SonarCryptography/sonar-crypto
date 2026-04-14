package org.sonarcrypto.utils.cognicrypt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonarcrypto.utils.cognicrypt.jimple.JimpleConvertingView;
import org.sonarcrypto.utils.jimple.mapper.ElementType;
import org.sonarcrypto.utils.jimple.mapper.LineMapping;
import org.sonarcrypto.utils.jimple.mapper.SourcePosition;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.core.model.Position;
import sootup.core.model.SourceType;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.jimple.frontend.JimpleAnalysisInputLocation;

class LocationReplacerInterceptorTest {

  /**
   * Verifies that interceptBody replaces the statement position using the full {@link
   * SourcePosition} (line + columns), not just the line number.
   */
  @Test
  void interceptBody_replacesStatementPositionPreservingColumnInfo() {
    Body body = loadGetValueBody();
    Stmt starting = body.getStmtGraph().getStartingStmt();
    int jimpleLine = starting.getPositionInfo().getStmtPosition().getFirstLine();

    // Build a mapping from this statement's current jimple line to a source position with columns
    var targetPos = new SourcePosition(42, 42, 7, 25);
    var mapping = new LineMapping(jimpleLine, ElementType.STATEMENT, "test", targetPos);
    var interceptor = new LocationReplacerInterceptor(Map.of(jimpleLine, mapping));

    Body.BodyBuilder builder = Body.builder(body, loadGetValueMethod().getModifiers());
    interceptor.interceptBody(builder, null);

    Position replaced =
        builder.getStmtGraph().getStartingStmt().getPositionInfo().getStmtPosition();
    assertThat(replaced.getFirstLine()).isEqualTo(42);
    assertThat(replaced.getFirstCol()).isEqualTo(7);
    assertThat(replaced.getLastLine()).isEqualTo(42);
    assertThat(replaced.getLastCol()).isEqualTo(25);
  }

  @Test
  void interceptBody_leavesUnmappedStatementsUnchanged() {
    Body body = loadGetValueBody();
    Stmt starting = body.getStmtGraph().getStartingStmt();
    int originalLine = starting.getPositionInfo().getStmtPosition().getFirstLine();

    // Intercept with an empty mapping — nothing should change
    var interceptor = new LocationReplacerInterceptor(Collections.emptyMap());
    Body.BodyBuilder builder = Body.builder(body, loadGetValueMethod().getModifiers());
    interceptor.interceptBody(builder, null);

    int lineAfter =
        builder.getStmtGraph().getStartingStmt().getPositionInfo().getStmtPosition().getFirstLine();
    assertThat(lineAfter).isEqualTo(originalLine);
  }

  // --- helpers ---

  private JavaSootMethod loadGetValueMethod() {
    JimpleAnalysisInputLocation loc = getTestJimpleInputLocation();
    JimpleConvertingView view = new JimpleConvertingView(loc);
    ClassType type = view.getIdentifierFactory().getClassType("JimpleTest");
    JavaSootClass cls = view.getClass(type).orElseThrow();
    return cls.getMethods().stream()
        .filter(m -> m.getName().equals("getValue"))
        .map(m -> (JavaSootMethod) m)
        .findFirst()
        .orElseThrow();
  }

  private Body loadGetValueBody() {
    return loadGetValueMethod().getBody();
  }

  private JimpleAnalysisInputLocation getTestJimpleInputLocation() {
    URL resource = getClass().getResource("/cognicrypt/jimple/JimpleTest.jimple");
    assertThat(resource).isNotNull();
    Path dir = new File(resource.getFile()).getParentFile().toPath();
    return new JimpleAnalysisInputLocation(dir, SourceType.Application, Collections.emptyList());
  }
}
