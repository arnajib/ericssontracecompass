/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *   Patrick Tasse - Support selection range
 *   Xavier Raynaud - Support filters tracking
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.trace;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.filter.ITmfFilter;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * Context of a trace, which is the representation of the "view" the user
 * currently has on this trace (window time range, selected time or time range).
 *
 * TODO could be extended to support the notion of current location too.
 *
 * FIXME Is this really the right place for the Editor File ?
 *
 * @author Alexandre Montplaisir
 * @since 1.0
 */
@NonNullByDefault
public final class TmfTraceContext {

    static final TmfTraceContext NULL_CONTEXT =
            new TmfTraceContext(new TmfTimeRange(TmfTimestamp.BIG_CRUNCH, TmfTimestamp.BIG_CRUNCH),
                    TmfTimeRange.NULL_RANGE, null, null);

    private final TmfTimeRange fSelection;
    private final TmfTimeRange fWindowRange;
    private final @Nullable IFile fEditorFile;
    private final @Nullable ITmfFilter fFilter;

    /**
     * Build a new trace context.
     *
     * @param selection
     *            The selected time range
     * @param windowRange
     *            The visible window's time range
     * @param editorFile
     *            The file representing the selected editor
     * @param filter
     *            The currently applied filter. 'null' for none.
     */
    public TmfTraceContext(TmfTimeRange selection, TmfTimeRange windowRange,
            @Nullable IFile editorFile, @Nullable ITmfFilter filter) {
        fSelection = selection;
        fWindowRange = windowRange;
        fEditorFile = editorFile;
        fFilter = filter;
    }

    /**
     * Return the time range representing the current active selection.
     *
     * @return The selected time range
     */
    public TmfTimeRange getSelectionRange() {
        return fSelection;
    }

    /**
     * Return the current window time range.
     *
     * @return The current window time range
     */
    public TmfTimeRange getWindowRange() {
        return fWindowRange;
    }


    /**
     * Get the editor's file
     *
     * @return The editor file
     */
    public @Nullable IFile getEditorFile() {
        return fEditorFile;
    }

    /**
     * Gets the filter applied to the current trace
     *
     * @return The current filter, or <code>null</code> if there is none
     */
    public @Nullable ITmfFilter getFilter() {
        return fFilter;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[fSelection=" + fSelection + //$NON-NLS-1$
                ", fWindowRange=" + fWindowRange + ']'; //$NON-NLS-1$
    }
}
