/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.ui.swtbot.tests;

import org.eclipse.tracecompass.analysis.os.linux.ui.swtbot.tests.latency.SystemCallLatencyTableAnalysisTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for UI on the lttng kernel perspective
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        SystemCallLatencyTableAnalysisTest.class
})
public class AllTests {

}
