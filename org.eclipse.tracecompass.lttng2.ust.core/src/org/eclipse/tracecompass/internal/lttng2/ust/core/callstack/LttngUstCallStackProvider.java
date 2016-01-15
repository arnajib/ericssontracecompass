/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *   Patrick Tasse - Add support for thread id
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.ust.core.callstack;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.lttng2.ust.core.trace.layout.LttngUst20EventLayout;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.lttng2.ust.core.trace.layout.ILttngUstEventLayout;
import org.eclipse.tracecompass.tmf.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;

import com.google.common.collect.ImmutableSet;

/**
 * Callstack provider for LTTng-UST traces.
 *
 * If the traces contains 'func_entry' and 'func_exit' event (see the
 * lttng-ust-cyg-profile manpage), AND contains vtid and procname contexts, we
 * can use this information to populate the TMF Callstack View.
 *
 * Granted, most UST traces will not contain this information. In this case,
 * this will simply build an empty state system, and the view will remain
 * unavailable.
 *
 * @author Alexandre Montplaisir
 */
public class LttngUstCallStackProvider extends CallStackStateProvider {

    /**
     * Version number of this state provider. Please bump this if you modify
     * the contents of the generated state history in some way.
     */
    private static final int VERSION = 2;

    /** Event names indicating function entry */
    private final @NonNull Set<String> funcEntryEvents;

    /** Event names indicating function exit */
    private final @NonNull Set<String> funcExitEvents;

    private final @NonNull ILttngUstEventLayout fLayout;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param trace
     *            The UST trace
     */
    public LttngUstCallStackProvider(@NonNull ITmfTrace trace) {
        super(trace);

        if (trace instanceof LttngUstTrace) {
            fLayout = ((LttngUstTrace) trace).getEventLayout();
        } else {
            /* For impostor trace types, assume they use the LTTng 2.0 layout */
            fLayout = LttngUst20EventLayout.getInstance();
        }

        funcEntryEvents = checkNotNull(ImmutableSet.of(
                fLayout.eventCygProfileFuncEntry(),
                fLayout.eventCygProfileFastFuncEntry()));

        funcExitEvents = checkNotNull(ImmutableSet.of(
                fLayout.eventCygProfileFuncExit(),
                fLayout.eventCygProfileFastFuncExit()));
    }

    // ------------------------------------------------------------------------
    // Methods from AbstractTmfStateProvider
    // ------------------------------------------------------------------------

    @Override
    public LttngUstCallStackProvider getNewInstance() {
        return new LttngUstCallStackProvider(getTrace());
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    // ------------------------------------------------------------------------
    // Methods from CallStackStateProvider
    // ------------------------------------------------------------------------

    /**
     * Check that this event contains the required information we need to be
     * used in the call stack view. We need at least the "procname" and "vtid"
     * contexts.
     */
    @Override
    protected boolean considerEvent(ITmfEvent event) {
        if (!(event instanceof CtfTmfEvent)) {
            return false;
        }
        ITmfEventField content = ((CtfTmfEvent) event).getContent();
        if (content.getField(fLayout.contextVtid()) == null ||
                content.getField(fLayout.contextProcname()) == null) {
            return false;
        }
        return true;
    }

    @Override
    public String functionEntry(ITmfEvent event) {
        String eventName = event.getName();
        if (!funcEntryEvents.contains(eventName)) {
            return null;
        }
        Long address = (Long) event.getContent().getField(fLayout.fieldAddr()).getValue();
        return Long.toHexString(address);
    }

    @Override
    public String functionExit(ITmfEvent event) {
        String eventName = event.getName();
        if (!funcExitEvents.contains(eventName)) {
            return null;
        }
        /*
         * The 'addr' field may or may not be present in func_exit events,
         * depending on if cyg-profile.so or cyg-profile-fast.so was used.
         */
        ITmfEventField field = event.getContent().getField(fLayout.fieldAddr());
        if (field == null) {
            return CallStackStateProvider.UNDEFINED;
        }
        Long address = (Long) field.getValue();
        return Long.toHexString(address);
    }

    @Override
    public String getThreadName(ITmfEvent event) {
        /* Class type and content was already checked if we get called here */
        ITmfEventField content = ((CtfTmfEvent) event).getContent();
        String procName = (String) content.getField(fLayout.contextProcname()).getValue();
        Long vtid = (Long) content.getField(fLayout.contextVtid()).getValue();

        if (procName == null || vtid == null) {
            throw new IllegalStateException();
        }

        return procName + '-' + vtid.toString();
    }

    @Override
    protected Long getThreadId(ITmfEvent event) {
        ITmfEventField content = ((CtfTmfEvent) event).getContent();
        return (Long) content.getField(fLayout.contextVtid()).getValue();
    }
}
