/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.latency.statistics;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.latency.statistics.SegmentStoreStatistics;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.latency.statistics.SystemCallLatencyStatisticsAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

/**
 * A tree viewer implementation for displaying latency statistics
 *
 * @author Bernd Hufmann
 *
 */
public class SystemCallLatencyStatisticsViewer extends AbstractSegmentStoreStatisticsViewer {

    private static final String SYSCALL_LEVEL = checkNotNull(Messages.LatencyStatistics_SyscallLevelName);

    /**
     * Constructor
     *
     * @param parent
     *            the parent composite
     */
    public SystemCallLatencyStatisticsViewer(Composite parent) {
        super(parent);
    }

    /**
     * Gets the statistics analysis module
     *
     * @return the statistics analysis module
     */
    @Override
    @Nullable protected TmfAbstractAnalysisModule createStatisticsAnalysiModule() {
        SystemCallLatencyStatisticsAnalysisModule module = new SystemCallLatencyStatisticsAnalysisModule();
        return module;
    }

    @Override
    @Nullable protected ITmfTreeViewerEntry updateElements(long start, long end, boolean isSelection) {
        if (isSelection || (start == end)) {
            return null;
        }

        TmfAbstractAnalysisModule analysisModule = getStatisticsAnalysisModule();

        if (getTrace() == null || !(analysisModule instanceof SystemCallLatencyStatisticsAnalysisModule)) {
            return null;
        }

        SystemCallLatencyStatisticsAnalysisModule module = (SystemCallLatencyStatisticsAnalysisModule) analysisModule;

        module.waitForCompletion();

        SegmentStoreStatistics entry = module.getTotalStats();

        TmfTreeViewerEntry root = new TmfTreeViewerEntry(""); //$NON-NLS-1$
        List<ITmfTreeViewerEntry> entryList = root.getChildren();

        TmfTreeViewerEntry child = new SegmentStoreStatisticsEntry(checkNotNull(Messages.LatencyStatistics_TotalLabel), checkNotNull(entry));
        entryList.add(child);

        HiddenTreeViewerEntry syscalls = new HiddenTreeViewerEntry(checkNotNull(SYSCALL_LEVEL));
        child.addChild(syscalls);

        Map<String, SegmentStoreStatistics> perSyscallStats = module.getPerSyscallStats();
        if (perSyscallStats != null) {
            Iterator<Entry<String, SegmentStoreStatistics>> stats = perSyscallStats.entrySet().iterator();
            while (stats.hasNext()) {
                Entry<String, SegmentStoreStatistics> statsEntry = stats.next();
                syscalls.addChild(new SegmentStoreStatisticsEntry(statsEntry.getKey(), statsEntry.getValue()));
            }
        }
        return root;
    }

}
