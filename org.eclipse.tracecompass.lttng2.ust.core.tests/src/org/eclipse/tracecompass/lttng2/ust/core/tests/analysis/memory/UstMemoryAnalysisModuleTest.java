/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Guilliano Molaire - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.ust.core.tests.analysis.memory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.lttng2.ust.core.analysis.memory.UstMemoryAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisRequirement;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for the {@link UstMemoryAnalysisModule}
 *
 * @author Guilliano Molaire
 */
public class UstMemoryAnalysisModuleTest {

    /** The analysis module */
    private UstMemoryAnalysisModule fUstAnalysisModule;

    /**
     * Set-up the test
     */
    @Before
    public void setup() {
        fUstAnalysisModule = new UstMemoryAnalysisModule();
    }

    /**
     * Test for {@link UstMemoryAnalysisModule#getAnalysisRequirements()}
     */
    @Ignore("Need to assign a trace to the module to check the requirements now")
    @Test
    public void testGetAnalysisRequirements() {
        Iterable<TmfAnalysisRequirement> requirements = fUstAnalysisModule.getAnalysisRequirements();

        assertNotNull(requirements);
        assertTrue(requirements.iterator().hasNext());
    }

}
