/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Julien Daoust & Najib Arbaoui - Initial API and implementation
 *                                 - Tests allocation and deallocation Kernel Memory
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.tests.kernelmemoryusage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelmemoryusage.KernelMemoryAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.Activator;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace.TmfXmlKernelTraceStub;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for the {@link KernelMemoryAnalysisModule} class
 *
 * @author Julien Daoust
 * @author Najib Arbaoui
 */
public class KernelMemoryStateProviderTest {

    private static final String KERNEL_MEMORY_USAGE_FILE = "testfiles/KernelMemoryAnalysis_testTrace.xml";
    private static final long PAGE_SIZE = 4096;
    private ITmfTrace fTrace;
    private KernelMemoryAnalysisModule fModule = null;
    private SortedMap<Long, Long> threadEvent = new TreeMap<>();

    private static void deleteSuppFiles(ITmfTrace trace) {
        /* Remove supplementary files */
        File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
        for (File file : suppDir.listFiles()) {
            file.delete();
        }
    }

    /**
     * Setup the trace for the tests
     */
    @Before
    public void setUp() {
        ITmfTrace trace = new TmfXmlKernelTraceStub();
        IPath filePath = Activator.getAbsoluteFilePath(KERNEL_MEMORY_USAGE_FILE);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            fail(e.getMessage());
        }
        deleteSuppFiles(trace);
        ((TmfTrace) trace).traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        /*
         * FIXME: Make sure this analysis is finished before running the CPU
         * analysis. This block can be removed once analysis dependency and
         * request precedence is implemented
         */
        IAnalysisModule module = null;
        for (IAnalysisModule mod : TmfTraceUtils.getAnalysisModulesOfClass(trace, KernelAnalysisModule.class)) {
            module = mod;
        }
        assertNotNull(module);
        module.schedule();
        module.waitForCompletion();
        /* End of the FIXME block */

        fModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelMemoryAnalysisModule.class, KernelMemoryAnalysisModule.ID);
        assertNotNull(fModule);
        fTrace = trace;
    }

    /**
     * Clean up
     */
    @After
    public void tearDown() {
        deleteSuppFiles(fTrace);
        fTrace.dispose();
    }

    /**
     * Test that the analysis executes without problems
     */
    @Test
    public void testAnalysisExecution() {
        /* Make sure the analysis hasn't run yet */
        assertNull(fModule.getStateSystem());

        /* Execute the analysis */
        assertTrue(TmfTestHelper.executeAnalysis(fModule));
        assertNotNull(fModule.getStateSystem());
    }

    /**
     * Test allocation and deallocation of kernel Memory
     */
    @Test
    public void testAllocationDeallocationMemory() {
        fModule.schedule();
        fModule.waitForCompletion();
        ITmfStateSystem ss = fModule.getStateSystem();
        assertNotNull(ss);
        assertEquals(1L, ss.getStartTime());
        assertEquals(30L, ss.getCurrentEndTime());
        long totalMemory = 0;

        threadEvent.put(1L, PAGE_SIZE); // kmem_mm_page_alloc at timestamp = 1
        threadEvent.put(2L, -PAGE_SIZE); // kmem_mm_page_free at timestamp = 2
        threadEvent.put(3L, -PAGE_SIZE); // kmem_mm_page_free at timestamp = 3
        threadEvent.put(17L, PAGE_SIZE); // kmem_mm_page_alloc at timestamp = 17
        threadEvent.put(22L, -PAGE_SIZE); // kmem_mm_page_free at timestamp = 22
        threadEvent.put(28L, -PAGE_SIZE); // kmem_mm_page_free at timestamp = 28
        threadEvent.put(29L, PAGE_SIZE); // kmem_mm_page_alloc at timestamp = 29
        threadEvent.put(30L, PAGE_SIZE); // kmem_mm_page_alloc at timestamp = 30

        // loop a Map and check if all allocation and deallocation of kernel
        // memory are done proprely
        for (Map.Entry<Long, Long> entry : threadEvent.entrySet()) {
            try {
                ITmfStateInterval kernelState = ss.querySingleState(entry.getKey(), 0);
                long value = kernelState.getStateValue().unboxLong();
                totalMemory += entry.getValue();
                assertEquals(totalMemory, value);

            } catch (StateSystemDisposedException | AttributeNotFoundException e) {

                fail(e.getMessage());
            }

        }

    }

    /**
     * Tests with values outside Interval TimeRangeException must be throwned
     */
    @Test(expected = TimeRangeException.class)
    public void testWithValueOutSideInterval() {

        fModule.schedule();
        fModule.waitForCompletion();
        ITmfStateSystem ss = fModule.getStateSystem();
        assertNotNull(ss);
        assertEquals(1L, ss.getStartTime());
        assertEquals(30L, ss.getCurrentEndTime());

        ITmfStateInterval kernelState1;
        ITmfStateInterval kernelState2;
        long value = 0;

        try {
            kernelState1 = ss.querySingleState(0L, 0);
            value = kernelState1.getStateValue().unboxLong();
            assertEquals(0L, value);
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            fail(e.getMessage());
        }

        try {
            kernelState2 = ss.querySingleState(31L, 0);
            value = kernelState2.getStateValue().unboxLong();
            assertEquals(0L, value);
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            fail(e.getMessage());
        }

    }
}