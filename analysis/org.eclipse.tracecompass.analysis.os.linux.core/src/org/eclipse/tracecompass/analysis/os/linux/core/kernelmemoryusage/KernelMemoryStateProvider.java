/**********************************************************************
 * Copyright (c) 2016 Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Samuel Gagnon - Initial implementation
 **********************************************************************/
package org.eclipse.tracecompass.analysis.os.linux.core.kernelmemoryusage;


import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Creates a state system and computes the total memory usage for all threads and
 * for each selected thread from a kernel trace.
 * The state provider also contains code that can query the state system.
 *
 * It requires only the 'sched_switch' events enabled on the trace.
 * @author Samuel Gagnon
 * @since 2.0
 *
 */
public class KernelMemoryStateProvider extends AbstractTmfStateProvider {

    /* Version of this state provider */
    private static final int VERSION = 1;

    private static final int PAGE_SIZE = 4096;

    private IKernelAnalysisEventLayout fLayout;

    /**
     * Constructor
     *
     * @param trace
     *            trace
     * @param layout
     *            layout
     */
    public KernelMemoryStateProvider(@NonNull ITmfTrace trace, IKernelAnalysisEventLayout layout) {
        super(trace, "Kernel:Memory"); //$NON-NLS-1$
        fLayout = layout;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new KernelMemoryStateProvider(this.getTrace(), this.fLayout);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        String name = event.getName();

        long inc;
        if (name.equals(fLayout.eventKmemPageAlloc())) {
            inc = PAGE_SIZE;
        } else if (name.equals(fLayout.eventKmemPageFree())) {
            inc = -PAGE_SIZE;
        } else {
            return;
        }

        try {
            ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
            long ts = event.getTimestamp().getValue();

            Integer tidField = KernelTidAspect.INSTANCE.resolve(event);
            String tid;
            if (tidField == null) {
                // this can be due to a race if the state system is not yet
                // built, or that the information is not yet available.
                tid = "other"; //$NON-NLS-1$
            } else {
                tid = tidField.toString();
            }

            int tidQuark = ss.getQuarkAbsoluteAndAdd(tid);
            ITmfStateValue previousMemoryValue = ss.queryOngoingState(tidQuark);
            if (previousMemoryValue.isNull()) {
                previousMemoryValue = TmfStateValue.newValueLong(0);
            }
            long currentMemoryValue = previousMemoryValue.unboxLong() + inc;
            ss.modifyAttribute(ts, TmfStateValue.newValueLong(currentMemoryValue), tidQuark);

            /**
             *  We add an attribute to keep the lowest memory value each thread.
             *  This quantity is used when we plot to avoid negative values.
             */
            int lowestMemoryQuark = ss.getQuarkRelativeAndAdd(tidQuark, KernelMemoryAnalysisModule.THREAD_LOWEST_MEMORY_VALUE);
            ITmfStateValue lowestMemoryValue = ss.queryOngoingState(lowestMemoryQuark);
            if (lowestMemoryValue.isNull()) {
                lowestMemoryValue = TmfStateValue.newValueLong(0);
            }

            if (lowestMemoryValue.unboxLong() > currentMemoryValue) {
                ss.modifyAttribute(ts, TmfStateValue.newValueLong(currentMemoryValue), lowestMemoryQuark);
            }
        } catch (AttributeNotFoundException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        }
    }

}
