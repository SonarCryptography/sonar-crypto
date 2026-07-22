package org.sonarcrypto.ccerror;

import crypto.analysis.errors.AbstractError;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.sonarcrypto.utils.sonar.FqClassName;
import org.sonarcrypto.utils.sonar.messagecrafter.MessageCrafter;

public class ConverterUtils {

  public static List<FlowEntry> executionFlow(AbstractError error) {
    final var relevantStatements = error.getSeed().getRelevantStatements();
    final var errorStatement = error.getErrorStatement();

    final var flow =
        relevantStatements.keys().stream()
            .map(
                stmt -> {
                  final var position =
                      org.sonarcrypto.utils.cognicrypt.crysl.ConverterUtils.intoPosition(stmt);

                  if (position == null) {
                    return null;
                  }
                  final var invokeExpr = stmt.getInvokeExpr();

                  final var messageCrafter = new MessageCrafter();

                  if (invokeExpr == null) {
                    messageCrafter.text("Relevant call");
                  } else {
                    messageCrafter.method(
                        invokeExpr.getDeclaredMethod().getDeclaringClass(),
                        invokeExpr.getDeclaredMethod().getName());
                  }

                  if (stmt == errorStatement) {
                    messageCrafter.text(" (unexpected call)");
                  }

                  return new FlowEntry(
                      new FqClassName(stmt.getMethod().getDeclaringClass().getFullyQualifiedName()),
                      position,
                      messageCrafter.toCraftedMessage());
                })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(FlowEntry::position).reversed())
            .toList();

    return flow.size() < 2 ? List.of(/* empty */ ) : flow;
  }
}
