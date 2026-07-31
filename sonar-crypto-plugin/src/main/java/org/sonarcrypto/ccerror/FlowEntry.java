package org.sonarcrypto.ccerror;

import org.jspecify.annotations.NullMarked;
import org.sonarcrypto.utils.sonar.FqClassName;
import org.sonarcrypto.utils.sonar.messagecrafter.CraftedMessage;
import sootup.core.model.Position;

@NullMarked
public record FlowEntry(FqClassName className, Position position, CraftedMessage message) {}
