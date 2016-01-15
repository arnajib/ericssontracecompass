/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.Attributes;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.StateValues;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * System call entry handler
 */
public class SysEntryHandler extends KernelEventHandler {

    /**
     * Constructor
     *
     * @param layout
     *            event layout
     */
    public SysEntryHandler(IKernelAnalysisEventLayout layout) {
        super(layout);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {
        Integer cpu = KernelEventHandlerUtils.getCpu(event);
        if (cpu == null) {
            return;
        }
        /* Assign the new system call to the process */
        int currentThreadNode = KernelEventHandlerUtils.getCurrentThreadNode(cpu, ss);
        int quark = ss.getQuarkRelativeAndAdd(currentThreadNode, Attributes.SYSTEM_CALL);
        ITmfStateValue value = TmfStateValue.newValueString(event.getName());
        long timestamp = KernelEventHandlerUtils.getTimestamp(event);
        ss.modifyAttribute(timestamp, value, quark);

        /* Put the process in system call mode */
        quark = ss.getQuarkRelativeAndAdd(currentThreadNode, Attributes.STATUS);
        value = StateValues.PROCESS_STATUS_RUN_SYSCALL_VALUE;
        ss.modifyAttribute(timestamp, value, quark);

        /* Put the CPU in system call (kernel) mode */
        int currentCPUNode = KernelEventHandlerUtils.getCurrentCPUNode(cpu, ss);
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, Attributes.STATUS);
        value = StateValues.CPU_STATUS_RUN_SYSCALL_VALUE;
        ss.modifyAttribute(timestamp, value, quark);
    }

}
