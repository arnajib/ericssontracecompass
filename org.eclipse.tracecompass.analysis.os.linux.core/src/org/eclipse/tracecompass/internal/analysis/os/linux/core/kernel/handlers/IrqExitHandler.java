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

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Irq Exit handler
 */
public class IrqExitHandler extends KernelEventHandler {

    /**
     * Constructor
     *
     * @param layout
     *            event layout
     */
    public IrqExitHandler(IKernelAnalysisEventLayout layout) {
        super(layout);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {
        Integer cpu = KernelEventHandlerUtils.getCpu(event);
        if( cpu == null ) {
            return;
        }
        int currentThreadNode = KernelEventHandlerUtils.getCurrentThreadNode(cpu, ss);
        Integer irqId = ((Long) event.getContent().getField(getLayout().fieldIrq()).getValue()).intValue();
        /* Put this IRQ back to inactive in the resource tree */
        int quark = ss.getQuarkRelativeAndAdd(KernelEventHandlerUtils.getNodeIRQs(ss), irqId.toString());
        TmfStateValue value = TmfStateValue.nullValue();
        long timestamp = KernelEventHandlerUtils.getTimestamp(event);
        ss.modifyAttribute(timestamp, value, quark);

        /* Set the previous process back to running */
        KernelEventHandlerUtils.setProcessToRunning(timestamp, currentThreadNode, ss);

        /* Set the CPU status back to running or "idle" */
        KernelEventHandlerUtils.cpuExitInterrupt(timestamp, cpu, ss);
    }
}
