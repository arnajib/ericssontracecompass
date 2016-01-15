/*******************************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Vincent Perot - Initial API and implementation
 *   Alexandre Montplaisir - Update to new ITmfEventAspect API
 *   Patrick Tasse - Make pcap aspects singletons
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.pcap.core.event.aspect;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.pcap.core.event.PcapEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

/**
 * The "packet reference" aspect for pcap events
 *
 * @author Alexandre Montplaisir
 */
public class PcapReferenceAspect implements ITmfEventAspect {

    /** Singleton instance */
    public static final PcapReferenceAspect INSTANCE = new PcapReferenceAspect();

    private PcapReferenceAspect() {
    }

    @Override
    public String getName() {
        return Messages.getMessage(Messages.PcapAspectName_Reference);
    }

    @Override
    public String getHelpText() {
        return EMPTY_STRING;
    }

    @Override
    public @Nullable String resolve(ITmfEvent event) {
        if (!(event instanceof PcapEvent)) {
            return null;
        }
        return ((PcapEvent) event).getReference();
    }
}
