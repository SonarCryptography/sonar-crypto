/*
 ********************************************************************************
 * Copyright (c) 2017 Fraunhofer IEM, Paderborn, Germany
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************
 */
package org.sonarcrypto.scanner.boomerang.scope.sootup;

import boomerang.scope.Method;
import boomerang.scope.Val;
import de.fraunhofer.iem.cryptoanalysis.handler.FrameworkHandler;
import org.jspecify.annotations.NonNull;
import org.sonarcrypto.scanner.boomerang.scope.sootup.jimple.JimpleUpMethod;
import org.sonarcrypto.scanner.boomerang.scope.sootup.jimple.JimpleUpVal;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.LongConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractBinopExpr;
import sootup.java.core.language.JavaJimple;

public class SootUpFrameworkHandler implements FrameworkHandler {
	
	@Override
	public Val createIntConstant(int value, @NonNull Method method) {
		if (method instanceof JimpleUpMethod jimpleUpMethod) {
			return new JimpleUpVal(IntConstant.getInstance(value), jimpleUpMethod);
		}
		
		throw new RuntimeException("Cannot create int constant without JimpleUpMethod");
	}
	
	@Override
	public Val createLongConstant(long value, @NonNull Method method) {
		if (method instanceof JimpleUpMethod jimpleUpMethod) {
			return new JimpleUpVal(LongConstant.getInstance(value), jimpleUpMethod);
		}
		
		throw new RuntimeException("Cannot create long constant without JimpleUpMethod");
	}
	
	@Override
	public Val createStringConstant(@NonNull String value, @NonNull Method method) {
		if (method instanceof JimpleUpMethod jimpleUpMethod) {
			StringConstant constant = JavaJimple.getInstance().newStringConstant(value);
			
			return new JimpleUpVal(constant, jimpleUpMethod);
		}
		
		throw new RuntimeException(
			"Cannot create String constant in SootUp without JimpleUpMethod");
	}
	
	@Override
	public boolean isBinaryExpr(@NonNull Val val) {
		if (val instanceof JimpleUpVal jimpleUpVal) {
			Value value = jimpleUpVal.getDelegate();
			
			return value instanceof AbstractBinopExpr;
		}
		
		return false;
	}
}
