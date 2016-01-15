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
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * Set Prio handler
 */
public class PiSetprioHandler extends KernelEventHandler {

    /**
     * Constructor
     * @param layout event layout
     */
    public PiSetprioHandler(IKernelAnalysisEventLayout layout) {
        super(layout);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {
        ITmfEventField content = event.getContent();
        Integer tid = ((Long) content.getField(getLayout().fieldTid()).getValue()).intValue();
        Integer prio = ((Long) content.getField(getLayout().fieldNewPrio()).getValue()).intValue();

        Integer updateThreadNode = ss.getQuarkRelativeAndAdd(KernelEventHandlerUtils.getNodeThreads(ss), tid.toString());

        /* Set the current prio for the new process */
        int quark = ss.getQuarkRelativeAndAdd(updateThreadNode, Attributes.PRIO);
        ITmfStateValue value = TmfStateValue.newValueInt(prio);
        ss.modifyAttribute(KernelEventHandlerUtils.getTimestamp(event), value, quark);
    }
}
