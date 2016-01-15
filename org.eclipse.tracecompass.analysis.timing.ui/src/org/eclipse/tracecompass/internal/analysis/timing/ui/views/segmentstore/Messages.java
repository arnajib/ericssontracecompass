/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   France Lapointe Nguyen - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.timing.ui.views.segmentstore;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.util.NLS;

/**
 * @author France Lapointe Nguyen
 * @since 2.0
 */
@NonNullByDefault({})
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.analysis.timing.ui.views.segmentstore.messages"; //$NON-NLS-1$

    /**
     * Name of the duration column
     */
    public static String SegmentStoreTableViewer_duration;

    /**
     * Name of the end time column
     */
    public static String SegmentStoreTableViewer_endTime;

    /**
     * Name of the start time column
     */
    public static String SegmentStoreTableViewer_startTime;

    /**
     * Title of action to goto start time time
     */
    public static String SegmentStoreTableViewer_goToStartEvent;

    /**
     * Title of action to goto end event
     */
    public static String SegmentStoreTableViewer_goToEndEvent;
    /**
     * Title of the scatter graph
     */
    public static String SegmentStoreScatterGraphViewer_title;

    /**
     * Title of the x axis of the scatter graph
     */
    public static String SegmentStoreScatterGraphViewer_xAxis;

    /**
     * Title of the y axis of the scatter graph
     */
    public static String SegmentStoreScatterGraphViewer_yAxis;

    /**
     * Legend
     */
    public static String SegmentStoreScatterGraphViewer_legend;

    /**
     * Name of the compacting job
     */
    public static String SegmentStoreScatterGraphViewer_compactTitle;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
