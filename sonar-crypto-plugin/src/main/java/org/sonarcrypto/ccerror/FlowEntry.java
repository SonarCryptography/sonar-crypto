package org.sonarcrypto.ccerror;

import org.sonarcrypto.utils.sonar.FqClassName;
import sootup.core.model.Position;

public record FlowEntry(FqClassName className, Position position, String message) {}
