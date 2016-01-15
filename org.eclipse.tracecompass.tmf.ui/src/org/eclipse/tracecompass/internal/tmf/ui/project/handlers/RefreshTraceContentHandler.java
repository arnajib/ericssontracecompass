/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial API and implementation.
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.ui.project.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Refresh the content of a trace in order to see new events.
 */
public class RefreshTraceContentHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        // Get the selection
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        final Iterator<Object> iterator = ((IStructuredSelection) selection).iterator();
        boolean refreshed = false;
        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (element instanceof TmfTraceElement) {
                TmfTraceElement trace = (TmfTraceElement) element;
                refreshTrace(trace);
                refreshed = true;
            }
        }

        if (refreshed) {
            return null;
        }

        IEditorPart activeEditor = HandlerUtil.getActiveEditorChecked(event);
        if (activeEditor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) activeEditor).getTrace();
            refreshTrace(trace);
        }

        return null;
    }

    private static void refreshTrace(TmfTraceElement t) {
        final TmfTraceElement traceElement = t.getElementUnderTraceFolder();
        ITmfTrace trace = traceElement.getTrace();
        if (trace != null) {
            refreshTrace(trace);
        }
    }

    private static void refreshTrace(ITmfTrace trace) {
        TmfTimeRange range = new TmfTimeRange(TmfTimestamp.BIG_BANG, TmfTimestamp.BIG_CRUNCH);
        TmfTraceRangeUpdatedSignal signal = new TmfTraceRangeUpdatedSignal(trace, trace, range);
        trace.broadcastAsync(signal);
    }

}
