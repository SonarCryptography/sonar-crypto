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

package org.sonarcrypto.scanner;

import boomerang.scope.DataFlowScope;
import crypto.analysis.CryptoAnalysisDataFlowScope;
import crypto.analysis.CryptoScanner;
import crypto.exceptions.CryptoAnalysisException;
import crypto.reporting.Reporter;
import crypto.reporting.ReporterFactory;
import crypto.visualization.Visualizer;
import crysl.rule.CrySLRule;
import de.fraunhofer.iem.cryptoanalysis.scope.CryptoAnalysisScope;
import de.fraunhofer.iem.framework.FrameworkSetup;
import de.fraunhofer.iem.framework.OpalSetup;
import de.fraunhofer.iem.framework.SootSetup;
import de.fraunhofer.iem.scanner.ScannerSettings;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonarcrypto.scanner.utils.CogniCryptSootUpSetup;
import org.sonarcrypto.scanner.utils.CogniCryptSootUpSetup.InputLocationType;
import sparse.SparsificationStrategy;

@NullMarked
public class CogniCryptScanner extends CryptoScanner {
	private static final Logger LOGGER = LoggerFactory.getLogger(CogniCryptScanner.class);
	
	private final InputLocationType inputLocationType;
	private final ScannerSettings settings;
	
	public CogniCryptScanner(
		InputLocationType inputLocationType,
		String applicationPath,
		String rulesetDirectory
	) {
		this.inputLocationType = inputLocationType;
		
		settings = new ScannerSettings();
		settings.setApplicationPath(applicationPath);
		settings.setRulesetPath(rulesetDirectory);
		settings.setReportFormats(new HashSet<>());
	}
	
	private CogniCryptScanner(
		InputLocationType inputLocationType,
		ScannerSettings settings
	) {
		this.inputLocationType = inputLocationType;
		this.settings = settings;
	}
	
	public InputLocationType getInputLocationType() {
		return inputLocationType;
	}
	
	public ScannerSettings getSettings() {
		return settings;
	}
	
	@Override
	public SparsificationStrategy<?, ?> getSparsificationStrategy() {
		//noinspection SwitchStatementWithTooFewBranches
		return switch(settings.getSparseStrategy()) {
			case NONE -> SparsificationStrategy.NONE;
			// case TYPE_BASED -> SparsificationStrategy.TYPE_BASED;
			// case ALIAS_AWARE -> SparsificationStrategy.ALIAS_AWARE;
			// TODO: If fixed in Boomerang, enable the options again
			default -> throw new UnsupportedOperationException(
				"Sparsification Strategy not supported"
			);
		};
	}
	
	@Override
	public int getTimeout() {
		return settings.getTimeout();
	}
	
	public void scan() {
		// Read rules
		LOGGER.info("Reading rules from {}", settings.getRulesetPath());
		Collection<CrySLRule> rules =
			readRules(settings.getRulesetPath(), settings.getAddClassPath());
		LOGGER.info("Found {} rules in {}", rules.size(), settings.getRulesetPath());
		
		// Initialize the reporters before the analysis to catch errors early
		Collection<Reporter> reporters =
			ReporterFactory.createReporters(
				settings.getReportFormats(), settings.getReportDirectory(), rules);
		if(settings.isVisualization() && settings.getReportDirectory() == null) {
			throw new IllegalArgumentException(
				"Cannot create visualization without existing report directory");
		}
		
		// Set up the framework
		DataFlowScope dataFlowScope =
			new CryptoAnalysisDataFlowScope(rules, settings.getIgnoredSections());
		CryptoAnalysisScope frameworkScope = initializeFrameworkScope(dataFlowScope);
		
		// Run the analysis
		super.scan(frameworkScope, rules, settings.getAddClassPath());
		
		// Report the errors
		for(Reporter reporter : reporters) {
			reporter.createAnalysisReport(
				super.getDiscoveredSeeds(), super.getCollectedErrors(), super.getStatistics());
		}
		
		// Create visualization
		if(settings.isVisualization()) {
			try {
				Visualizer visualizer = new Visualizer(settings.getReportDirectory());
				visualizer.createVisualization(getDiscoveredSeeds());
			}
			catch(IOException e) {
				throw new CryptoAnalysisException(
					"Couldn't create visualization: " + e.getMessage());
			}
		}
	}
	
	private CryptoAnalysisScope initializeFrameworkScope(DataFlowScope dataFlowScope) {
		FrameworkSetup frameworkSetup = setupFramework(dataFlowScope);
		frameworkSetup.initializeFramework();
		
		super.getAnalysisReporter().beforeCallGraphConstruction();
		CryptoAnalysisScope frameworkScope = frameworkSetup.createFrameworkScope();
		super.getAnalysisReporter()
			.afterCallGraphConstruction(frameworkScope.asFrameworkScope().getCallGraph());
		
		return frameworkScope;
	}
	
	private FrameworkSetup setupFramework(DataFlowScope dataFlowScope) {
		return switch(settings.getFramework()) {
			case SOOT -> new SootSetup(
				settings.getApplicationPath(),
				settings.getCallGraph(),
				settings.getAddClassPath(),
				dataFlowScope);
			case SOOT_UP -> new CogniCryptSootUpSetup(
				inputLocationType,
				settings.getApplicationPath(),
				settings.getCallGraph(),
				dataFlowScope
			);
			case OPAL -> new OpalSetup(
				settings.getApplicationPath(), settings.getCallGraph(), dataFlowScope);
		};
	}
}
