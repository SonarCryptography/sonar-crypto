package org.sonarcrypto.ccerror;

import org.sonarcrypto.utils.sonar.FqClassName;
import org.sonarcrypto.utils.sonar.messagecrafter.CraftedMessage;
import sootup.core.model.Position;

public record FlowEntry(FqClassName className, Position position, CraftedMessage message) {}
