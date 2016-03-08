/**********************************************************************
 * Copyright (c) 2016 Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mahdi Zolnouri - Initial implementation
 *   Wassim Nasrallah - Initial implementatio
 **********************************************************************/
package org.eclipse.tracecompass.analysis.os.linux.ui.views.kernelmemoryusage;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.Attributes;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelmemoryusage.KernelMemoryAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractTmfTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * Tree viewer to select which process to display in the kernel memory usage chart.
 *
 * @author Mahdi Zolnouri
 * @author Wassim Nasrallah
 */
public class KernelMemoryUsageComposite extends AbstractTmfTreeViewer {

    /* Timeout between to wait for in the updateElements method */
    private KernelMemoryAnalysisModule fModule = null;
    private String fSelectedThread = null;
    private static final String[] COLUMN_NAMES = new String[] {
            Messages.KernelMemoryUsageComposite_ColumnTID,
            Messages.KernelMemoryUsageComposite_ColumnProcess
    };

    /* A map that saves the mapping of a thread ID to its executable name */
    private final Map<String, String> fProcessNameMap = new HashMap<>();

    /** Provides label for the Kernel memory usage tree viewer cells */
    protected static class KernelMemoryLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            KernelMemoryUsageEntry obj = (KernelMemoryUsageEntry) element;
            if (columnIndex == 0) {
                return obj.getTid();
            } else if (columnIndex == 1) {
                return obj.getProcessName();
            }
            return element.toString();
        }
    }

    /**
     * Constructor
     *
     * @param parent
     *            The parent composite that holds this viewer
     */
    public KernelMemoryUsageComposite(Composite parent) {
        super(parent, false);
        setLabelProvider(new KernelMemoryLabelProvider());
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return new ITmfTreeColumnDataProvider() {

            @Override
            public List<TmfTreeColumnData> getColumnData() {
                /* All columns are sortable */
                List<TmfTreeColumnData> columns = new ArrayList<>();
                TmfTreeColumnData column = new TmfTreeColumnData(COLUMN_NAMES[0]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(Viewer viewer, Object e1, Object e2) {
                        KernelMemoryUsageEntry n1 = (KernelMemoryUsageEntry) e1;
                        KernelMemoryUsageEntry n2 = (KernelMemoryUsageEntry) e2;

                        return n1.getTid().compareTo(n2.getTid());
                    }
                });
                columns.add(column);
                column = new TmfTreeColumnData(COLUMN_NAMES[1]);
                column.setComparator(new ViewerComparator() {
                    @Override
                    public int compare(Viewer viewer, Object e1, Object e2) {
                        KernelMemoryUsageEntry n1 = (KernelMemoryUsageEntry) e1;
                        KernelMemoryUsageEntry n2 = (KernelMemoryUsageEntry) e2;

                        return n1.getProcessName().compareTo(n2.getProcessName());
                    }
                });
                columns.add(column);
                return columns;
            }
        };
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    protected void contentChanged(ITmfTreeViewerEntry rootEntry) {
        String selectedThread = fSelectedThread;
        if (selectedThread != null) {
            /* Find the selected thread among the inputs */
            for (ITmfTreeViewerEntry entry : rootEntry.getChildren()) {
                if (entry instanceof KernelMemoryUsageEntry) {
                    if (selectedThread.equals(((KernelMemoryUsageEntry) entry).getTid())) {
                        List<ITmfTreeViewerEntry> list = checkNotNull(Collections.singletonList(entry));
                        super.setSelection(list);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void initializeDataSource() {
        /* Should not be called while trace is still null */
        ITmfTrace trace = checkNotNull(getTrace());

        fModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelMemoryAnalysisModule.class, KernelMemoryAnalysisModule.ID);
        if (fModule == null) {
            return;
        }
        fModule.schedule();
        fModule.waitForInitialization();
        fProcessNameMap.clear();
    }

    @Override
    protected ITmfTreeViewerEntry updateElements(long start, long end, boolean isSelection) {
        if (isSelection || (start == end)) {
            return null;
        }
        if (getTrace() == null || fModule == null) {
            return null;
        }
        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return null;
        }
        ss.waitUntilBuilt();
        TmfTreeViewerEntry root = new TmfTreeViewerEntry(""); //$NON-NLS-1$
        List<ITmfTreeViewerEntry> entryList = root.getChildren();

        try {
            List<ITmfStateInterval> kernelState = ss.queryFullState(start);
            List<Integer> threadQuarkList = ss.getSubAttributes(-1, false);

            for (Integer threadQuark : threadQuarkList) {
                ITmfStateInterval threadMemoryInterval = kernelState.get(threadQuark);
                if (threadMemoryInterval.getEndTime() < end) {
                    String tid = ss.getAttributeName(threadQuark);
                    String procname = getProcessName(tid);
                    KernelMemoryUsageEntry obj = new KernelMemoryUsageEntry(tid, procname);
                    entryList.add(obj);
                }
            }
        } catch (StateSystemDisposedException | AttributeNotFoundException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        }
        return root;
    }

    /*
     * Get the process name from its TID by using the LTTng kernel analysis
     * module
     */
    private String getProcessName(String tid) {
        String execName = fProcessNameMap.get(tid);
        if (execName != null) {
            return execName;
        }
        ITmfTrace trace = checkNotNull(getTrace());
        ITmfStateSystem kernelSs = checkNotNull(TmfStateSystemAnalysisModule.getStateSystem(trace, KernelAnalysisModule.ID));
        try {
            int cpusNode = kernelSs.getQuarkAbsolute(Attributes.THREADS);

            /* Get the quarks for each cpu */
            List<Integer> cpuNodes = kernelSs.getSubAttributes(cpusNode, false);

            execName = getCurrentStateIntervalValue(tid, kernelSs, cpuNodes);
            if(execName != null) {
                return execName;
            }
        } catch (AttributeNotFoundException e) {
            /* can't find the process name, just return the tid instead */
        }
        return tid;
    }

    private String getCurrentStateIntervalValue(String tid, ITmfStateSystem kernelSs, List<Integer> cpuNodes) {
        String currentStateIntervalValue = null;
        for (Integer tidQuark : cpuNodes) {
            if (kernelSs.getAttributeName(tidQuark).equals(tid)) {
                int execNameQuark;
                List<ITmfStateInterval> execNameIntervals;
                try {
                    execNameQuark = kernelSs.getQuarkRelative(tidQuark, Attributes.EXEC_NAME);
                    execNameIntervals = StateSystemUtils.queryHistoryRange(kernelSs, execNameQuark, getStartTime(), getEndTime());
                } catch (AttributeNotFoundException e) {
                    /* No information on this thread (yet?), skip it for now */
                    continue;
                } catch (StateSystemDisposedException e) {
                    /* State system is closing down, no point continuing */
                    break;
                }
                for (ITmfStateInterval execNameInterval : execNameIntervals) {
                    if (!execNameInterval.getStateValue().isNull() &&
                            execNameInterval.getStateValue().getType().equals(ITmfStateValue.Type.STRING)) {
                        /*  Here we retrieve the String contained in the state value represented by this interval */
                        currentStateIntervalValue = execNameInterval.getStateValue().unboxStr();
                        fProcessNameMap.put(tid, currentStateIntervalValue);
                        return currentStateIntervalValue;
                    }
                }
            }
        }
        return currentStateIntervalValue;

    }
    /**
     * Set the currently selected thread ID
     *
     * @param tid
     *            The selected thread ID
     */
    public void setSelectedThread(String tid) {
        fSelectedThread = tid;
    }
}