/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexis Cabana-Loriaux - Initial API and implementation
 *
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.viewers.piecharts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

/**
 * Implementation of the IPieChartViewerState interface to represent the state
 * of the layout when there is content currently selected.
 *
 * @author Alexis Cabana-Loriaux
 * @since 2.0
 *
 */
public class PieChartViewerStateContentSelected implements IPieChartViewerState {

    /**
     * Default constructor
     *
     * @param context
     *            The context to apply the changes
     */
    public PieChartViewerStateContentSelected(final TmfPieChartViewer context) {
        if (context.isDisposed()) {
            return;
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (context) {
                    if (!context.isDisposed()) {
                        context.updateGlobalPieChart();
                        context.updateTimeRangeSelectionPieChart();
                        context.getTimeRangePC().redraw();
                        context.getGlobalPC().getLegend().setPosition(SWT.BOTTOM);
                        context.layout();
                    }
                }
            }
        });

    }

    @Override
    public void newSelection(final TmfPieChartViewer context) {
        if (context.isDisposed()) {
            return;
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (context) {
                    if (!context.isDisposed()) {
                        context.updateTimeRangeSelectionPieChart();
                        context.getTimeRangePC().redraw();
                        context.layout();
                    }
                }
            }
        });
    }

    @Override
    public void newEmptySelection(final TmfPieChartViewer context) {
        context.setCurrentState(new PieChartViewerStateNoContentSelected(context));
    }

    @Override
    public void newGlobalEntries(final TmfPieChartViewer context) {
        // when new global entries, don't show the selection pie-chart anymore
        context.setCurrentState(new PieChartViewerStateNoContentSelected(context));
    }
}
