/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Bernd Hufmann - Updated signal handling
 *   Marc-Andre Laperle - Map from binary file
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views.callstack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tracecompass.internal.tmf.core.callstack.FunctionNameMapper;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue.Type;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfNanoTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampDelta;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphTimeListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphContentProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphSelection;
import org.eclipse.ui.IEditorPart;

/**
 * Main implementation for the Call Stack view
 *
 * @author Patrick Tasse
 */
public class CallStackView extends AbstractTimeGraphView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /** View ID. */
    public static final String ID = "org.eclipse.linuxtools.tmf.ui.views.callstack"; //$NON-NLS-1$

    private static final String[] COLUMN_NAMES = new String[] {
            Messages.CallStackView_FunctionColumn,
            Messages.CallStackView_DepthColumn,
            Messages.CallStackView_EntryTimeColumn,
            Messages.CallStackView_ExitTimeColumn,
            Messages.CallStackView_DurationColumn
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.CallStackView_ThreadColumn
    };

    /** Timeout between updates in the build thread in ms */
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    // Fraction of a function duration to be added as spacing
    private static final double SPACING_RATIO = 0.01;

    private static final Image THREAD_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/thread_obj.gif"); //$NON-NLS-1$
    private static final Image STACKFRAME_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/stckframe_obj.gif"); //$NON-NLS-1$

    private static final String IMPORT_MAPPING_ICON_PATH = "icons/etool16/import.gif"; //$NON-NLS-1$
    private static final String IMPORT_BINARY_ICON_PATH = "icons/obj16/binaries_obj.gif"; //$NON-NLS-1$

    private static final ImageDescriptor SORT_BY_NAME_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_alpha.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_NAME_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_alpha_rev.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_ID_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_num.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_ID_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_num_rev.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_TIME_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_time.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_TIME_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_time_rev.gif"); //$NON-NLS-1$
    private static final String SORT_OPTION_KEY = "sort.option"; //$NON-NLS-1$

    private enum SortOption {
        BY_NAME, BY_NAME_REV, BY_ID, BY_ID_REV, BY_TIME, BY_TIME_REV
    }

    private @NonNull SortOption fSortOption = SortOption.BY_NAME;
    private @NonNull Comparator<ITimeGraphEntry> fThreadComparator = new ThreadNameComparator(false);
    private Action fSortByNameAction;
    private Action fSortByIdAction;
    private Action fSortByTimeAction;

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    /** The map to map function addresses to function names */
    private Map<String, String> fNameMapping;

    // The next event action
    private Action fNextEventAction;

    // The previous event action
    private Action fPrevEventAction;

    // The next item action
    private Action fNextItemAction;

    // The previous item action
    private Action fPreviousItemAction;

    // The action to import a function-name mapping file
    private Action fImportMappingAction;

    // The action to import a binary file mapping */
    private Action fImportBinaryFileMappingAction;

    // The saved time sync. signal used when switching off the pinning of a view
    private TmfSelectionRangeUpdatedSignal fSavedTimeSyncSignal;

    // The saved window range signal used when switching off the pinning of
    // a view
    private TmfWindowRangeUpdatedSignal fSavedRangeSyncSignal;

    // ------------------------------------------------------------------------
    // Classes
    // ------------------------------------------------------------------------

    private class TraceEntry extends TimeGraphEntry {
        public TraceEntry(String name, long startTime, long endTime) {
            super(name, startTime, endTime);
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }
    }

    private class ThreadEntry extends TimeGraphEntry {
        // The call stack quark
        private final int fCallStackQuark;
        // The state system from which this entry comes
        private final ITmfStateSystem fSS;
        // The thread id
        private final long fThreadId;

        public ThreadEntry(ITmfStateSystem ss, String name, long threadId, int callStackQuark, long startTime, long endTime) {
            super(name, startTime, endTime);
            fCallStackQuark = callStackQuark;
            fThreadId = threadId;
            fSS = ss;
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }

        public int getCallStackQuark() {
            return fCallStackQuark;
        }

        public long getThreadId() {
            return fThreadId;
        }

        @Nullable
        public ITmfStateSystem getStateSystem() {
            return fSS;
        }
    }

    private class CallStackComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            if (o1 instanceof ThreadEntry && o2 instanceof ThreadEntry) {
                return fThreadComparator.compare(o1, o2);
            }
            return 0;
        }
    }

    private class ThreadNameComparator implements Comparator<ITimeGraphEntry> {
        private boolean reverse;

        public ThreadNameComparator(boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            return reverse ? o2.getName().compareTo(o1.getName()) :
                    o1.getName().compareTo(o2.getName());
        }
    }

    private class ThreadIdComparator implements Comparator<ITimeGraphEntry> {
        private boolean reverse;

        public ThreadIdComparator(boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            ThreadEntry t1 = (ThreadEntry) o1;
            ThreadEntry t2 = (ThreadEntry) o2;
            return reverse ? Long.compare(t2.getThreadId(), t1.getThreadId()) :
                    Long.compare(t1.getThreadId(), t2.getThreadId());
        }
    }

    private class ThreadTimeComparator implements Comparator<ITimeGraphEntry> {
        private boolean reverse;

        public ThreadTimeComparator(boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            return reverse ? Long.compare(o2.getStartTime(), o1.getStartTime()) :
                    Long.compare(o1.getStartTime(), o2.getStartTime());
        }
    }

    private class CallStackTreeLabelProvider extends TreeLabelProvider {

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == 0) {
                if (element instanceof ThreadEntry) {
                    return THREAD_IMAGE;
                } else if (element instanceof CallStackEntry) {
                    CallStackEntry entry = (CallStackEntry) element;
                    if (entry.getFunctionName().length() > 0) {
                        return STACKFRAME_IMAGE;
                    }
                }
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof CallStackEntry) {
                CallStackEntry entry = (CallStackEntry) element;
                if (columnIndex == 0) {
                    return entry.getFunctionName();
                } else if (columnIndex == 1 && entry.getFunctionName().length() > 0) {
                    int depth = entry.getStackLevel();
                    return Integer.toString(depth);
                } else if (columnIndex == 2 && entry.getFunctionName().length() > 0) {
                    ITmfTimestamp ts = new TmfTimestamp(entry.getFunctionEntryTime(), ITmfTimestamp.NANOSECOND_SCALE);
                    return ts.toString();
                } else if (columnIndex == 3 && entry.getFunctionName().length() > 0) {
                    ITmfTimestamp ts = new TmfTimestamp(entry.getFunctionExitTime(), ITmfTimestamp.NANOSECOND_SCALE);
                    return ts.toString();
                } else if (columnIndex == 4 && entry.getFunctionName().length() > 0) {
                    ITmfTimestamp ts = new TmfTimestampDelta(entry.getFunctionExitTime() - entry.getFunctionEntryTime(), ITmfTimestamp.NANOSECOND_SCALE);
                    return ts.toString();
                }
            } else if (element instanceof ITimeGraphEntry) {
                if (columnIndex == 0) {
                    return ((ITimeGraphEntry) element).getName();
                }
            }
            return ""; //$NON-NLS-1$
        }

    }

    private class CallStackFilterContentProvider extends TimeGraphContentProvider {
        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof TraceEntry) {
                return super.hasChildren(element);
            }
            return false;
        }

        @Override
        public ITimeGraphEntry[] getChildren(Object parentElement) {
            if (parentElement instanceof TraceEntry) {
                return super.getChildren(parentElement);
            }
            return new ITimeGraphEntry[0];
        }
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public CallStackView() {
        super(ID, new CallStackPresentationProvider());
        ((CallStackPresentationProvider) getPresentationProvider()).setCallStackView(this);
        setTreeColumns(COLUMN_NAMES);
        setTreeLabelProvider(new CallStackTreeLabelProvider());
        setEntryComparator(new CallStackComparator());
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterContentProvider(new CallStackFilterContentProvider());
        setFilterLabelProvider(new CallStackTreeLabelProvider());
    }

    // ------------------------------------------------------------------------
    // ViewPart
    // ------------------------------------------------------------------------

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        getTimeGraphViewer().addTimeListener(new ITimeGraphTimeListener() {
            @Override
            public void timeSelected(TimeGraphTimeEvent event) {
                synchingToTime(event.getBeginTime());
            }
        });

        getTimeGraphCombo().getTreeViewer().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
                if (selection instanceof CallStackEntry) {
                    CallStackEntry entry = (CallStackEntry) selection;
                    if (entry.getFunctionName().length() > 0) {
                        long entryTime = entry.getFunctionEntryTime();
                        long exitTime = entry.getFunctionExitTime();
                        long spacingTime = (long) ((exitTime - entryTime) * SPACING_RATIO);
                        entryTime -= spacingTime;
                        exitTime += spacingTime;
                        TmfTimeRange range = new TmfTimeRange(new TmfNanoTimestamp(entryTime), new TmfNanoTimestamp(exitTime));
                        broadcast(new TmfWindowRangeUpdatedSignal(CallStackView.this, range));
                        getTimeGraphViewer().setStartFinishTime(entryTime, exitTime);
                        startZoomThread(entryTime, exitTime);
                    }
                }
            }
        });

        getTimeGraphViewer().getTimeGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                TimeGraphControl timeGraphControl = getTimeGraphViewer().getTimeGraphControl();
                ISelection selection = timeGraphControl.getSelection();
                if (selection instanceof TimeGraphSelection) {
                    Object o = ((TimeGraphSelection) selection).getFirstElement();
                    if (o instanceof CallStackEvent) {
                        CallStackEvent event = (CallStackEvent) o;
                        long startTime = event.getTime();
                        long endTime = startTime + event.getDuration();
                        long spacingTime = (long) ((endTime - startTime) * SPACING_RATIO);
                        startTime -= spacingTime;
                        endTime += spacingTime;
                        TmfTimeRange range = new TmfTimeRange(new TmfNanoTimestamp(startTime), new TmfNanoTimestamp(endTime));
                        broadcast(new TmfWindowRangeUpdatedSignal(CallStackView.this, range));
                        getTimeGraphViewer().setStartFinishTime(startTime, endTime);
                        startZoomThread(startTime, endTime);
                    }
                }
            }
        });

        contributeToActionBars();
        createContextMenu();
        loadSortOption();

        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
            if (trace != null) {
                traceSelected(new TmfTraceSelectedSignal(this, trace));
            }
        }
    }

    /**
     * Handler for the selection range signal.
     *
     * @param signal
     *            The incoming signal
     * @since 1.0
     */
    @Override
    @TmfSignalHandler
    public void selectionRangeUpdated(final TmfSelectionRangeUpdatedSignal signal) {

        fSavedTimeSyncSignal = isPinned() ? new TmfSelectionRangeUpdatedSignal(signal.getSource(), signal.getBeginTime(), signal.getEndTime()) : null;

        if (signal.getSource() == this || getTrace() == null || isPinned()) {
            return;
        }
        final long beginTime = signal.getBeginTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
        final long endTime = signal.getEndTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (getTimeGraphCombo().isDisposed()) {
                    return;
                }
                if (beginTime == endTime) {
                    getTimeGraphViewer().setSelectedTime(beginTime, true);
                } else {
                    getTimeGraphViewer().setSelectionRange(beginTime, endTime, true);
                }
                synchingToTime(beginTime);
                startZoomThread(getTimeGraphViewer().getTime0(), getTimeGraphViewer().getTime1());
                List<TimeGraphEntry> entryList = getEntryList(getTrace());
                if (entryList == null) {
                    return;
                }
                TimeGraphViewer viewer = getTimeGraphViewer();
                for (TimeGraphEntry traceEntry : entryList) {
                    for (ITimeGraphEntry child : traceEntry.getChildren()) {
                        ThreadEntry threadEntry = (ThreadEntry) child;
                        ITmfStateSystem ss = threadEntry.getStateSystem();
                        if (ss == null || beginTime < ss.getStartTime() || beginTime > ss.getCurrentEndTime()) {
                            continue;
                        }
                        try {
                            int quark = threadEntry.getCallStackQuark();
                            ITmfStateInterval stackInterval = ss.querySingleState(beginTime, quark);
                            if (beginTime == stackInterval.getStartTime()) {
                                int stackLevel = stackInterval.getStateValue().unboxInt();
                                ITimeGraphEntry selectedEntry = threadEntry.getChildren().get(Math.max(0, stackLevel - 1));
                                getTimeGraphCombo().setSelection(selectedEntry);
                                viewer.getTimeGraphControl().fireSelectionChanged();
                                break;
                            }
                        } catch (AttributeNotFoundException | TimeRangeException | StateSystemDisposedException | StateValueTypeException e) {
                            Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
                        }
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    @Override
    protected void buildEventList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return;
        }
        AbstractCallStackAnalysis module = getCallStackModule(trace);
        if (module == null) {
            addUnavailableEntry(trace, parentTrace);
            return;
        }
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null) {
            addUnavailableEntry(trace, parentTrace);
            return;
        }

        Map<ITmfTrace, TraceEntry> traceEntryMap = new HashMap<>();
        Map<Integer, ThreadEntry> threadEntryMap = new HashMap<>();
        String[] threadPaths = module.getThreadsPattern();

        long start = ss.getStartTime();

        boolean complete = false;
        while (!complete) {
            if (monitor.isCanceled()) {
                return;
            }
            complete = ss.waitUntilBuilt(BUILD_UPDATE_TIMEOUT);
            if (ss.isCancelled()) {
                return;
            }
            long end = ss.getCurrentEndTime();
            if (start == end && !complete) { // when complete execute one last time regardless of end time
                continue;
            }
            List<Integer> threadQuarks = ss.getQuarks(threadPaths);
            TraceEntry traceEntry = traceEntryMap.get(trace);
            if (traceEntry == null) {
                traceEntry = new TraceEntry(trace.getName(), start, end + 1);
                traceEntryMap.put(trace, traceEntry);
                traceEntry.sortChildren(fThreadComparator);
                addToEntryList(parentTrace, Collections.singletonList(traceEntry));
            } else {
                traceEntry.updateEndTime(end);
            }
            for (int i = 0; i < threadQuarks.size(); i++) {
                if (monitor.isCanceled()) {
                    return;
                }
                int threadQuark = threadQuarks.get(i);
                try {
                    String[] callStackPath = module.getCallStackPath();
                    int callStackQuark = ss.getQuarkRelative(threadQuark, callStackPath);
                    String threadName = ss.getAttributeName(threadQuark);
                    long threadEnd = end + 1;
                    ITmfStateInterval endInterval = ss.querySingleState(ss.getCurrentEndTime(), callStackQuark);
                    if (endInterval.getStateValue().isNull() && endInterval.getStartTime() != ss.getStartTime()) {
                        threadEnd = endInterval.getStartTime();
                    }
                    ThreadEntry threadEntry = threadEntryMap.get(threadQuark);
                    if (threadEntry == null) {
                        long threadId = ss.querySingleState(ss.getCurrentEndTime(), threadQuark).getStateValue().unboxLong();
                        long threadStart = start;
                        ITmfStateInterval startInterval = ss.querySingleState(start, callStackQuark);
                        if (startInterval.getStateValue().isNull()) {
                            threadStart = Math.min(startInterval.getEndTime() + 1, end + 1);
                        }
                        threadEntry = new ThreadEntry(ss, threadName, threadId, callStackQuark, threadStart, threadEnd);
                        threadEntryMap.put(threadQuark, threadEntry);
                        traceEntry.addChild(threadEntry);
                    } else {
                        threadEntry.updateEndTime(threadEnd);
                    }
                    int level = 1;
                    for (int stackLevelQuark : ss.getSubAttributes(callStackQuark, false)) {
                        if (level > threadEntry.getChildren().size()) {
                            CallStackEntry callStackEntry = new CallStackEntry(threadName, stackLevelQuark, level, trace, ss);
                            threadEntry.addChild(callStackEntry);
                        }
                        level++;
                    }
                } catch (AttributeNotFoundException e) {
                    Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
                } catch (StateSystemDisposedException e) {
                    /* Ignored */
                }
            }
            if (parentTrace == getTrace()) {
                synchronized (this) {
                    setStartTime(getStartTime() == SWT.DEFAULT ? start : Math.min(getStartTime(), start));
                    setEndTime(getEndTime() == SWT.DEFAULT ? end + 1 : Math.max(getEndTime(), end + 1));
                }
                synchingToTime(getTimeGraphViewer().getSelectionBegin());
                refresh();
            }
            for (ITimeGraphEntry threadEntry : traceEntry.getChildren()) {
                for (ITimeGraphEntry callStackEntry : threadEntry.getChildren()) {
                    if (monitor.isCanceled()) {
                        return;
                    }
                    buildStatusEvents(parentTrace, (CallStackEntry) callStackEntry, monitor, start, end);
                }
            }
            start = end;
        }
    }

    private void addUnavailableEntry(ITmfTrace trace, ITmfTrace parentTrace) {
        String name = Messages.CallStackView_StackInfoNotAvailable + ' ' + '(' + trace.getName() + ')';
        TraceEntry unavailableEntry = new TraceEntry(name, 0, 0);
        addToEntryList(parentTrace, Collections.singletonList(unavailableEntry));
        if (parentTrace == getTrace()) {
            refresh();
        }
    }

    private void buildStatusEvents(ITmfTrace trace, CallStackEntry entry, @NonNull IProgressMonitor monitor, long start, long end) {
        ITmfStateSystem ss = entry.getStateSystem();
        long resolution = Math.max(1, (end - ss.getStartTime()) / getDisplayWidth());
        List<ITimeEvent> eventList = getEventList(entry, start, end + 1, resolution, monitor);
        if (eventList != null) {
            for (ITimeEvent event : eventList) {
                entry.addEvent(event);
            }
        }
        if (trace == getTrace()) {
            redraw();
        }
    }

    /**
     * @since 2.0
     */
    @Override
    protected final List<ITimeEvent> getEventList(TimeGraphEntry tgentry, long startTime, long endTime, long resolution, IProgressMonitor monitor) {
        if (!(tgentry instanceof CallStackEntry)) {
            return null;
        }
        CallStackEntry entry = (CallStackEntry) tgentry;
        ITmfStateSystem ss = entry.getStateSystem();
        long start = Math.max(startTime, ss.getStartTime());
        long end = Math.min(endTime, ss.getCurrentEndTime() + 1);
        if (end <= start) {
            return null;
        }
        List<ITimeEvent> eventList = null;
        try {
            List<ITmfStateInterval> stackIntervals = StateSystemUtils.queryHistoryRange(ss, entry.getQuark(), start, end - 1, resolution, monitor);
            eventList = new ArrayList<>(stackIntervals.size());
            long lastEndTime = -1;
            boolean lastIsNull = true;
            for (ITmfStateInterval statusInterval : stackIntervals) {
                if (monitor.isCanceled()) {
                    return null;
                }
                long time = statusInterval.getStartTime();
                long duration = statusInterval.getEndTime() - time + 1;
                if (!statusInterval.getStateValue().isNull()) {
                    final int modulo = CallStackPresentationProvider.NUM_COLORS / 2;
                    int value = statusInterval.getStateValue().toString().hashCode() % modulo + modulo;
                    eventList.add(new CallStackEvent(entry, time, duration, value));
                    lastIsNull = false;
                } else {
                    if (lastEndTime == -1) {
                        // add null event if it intersects the start time
                        eventList.add(new NullTimeEvent(entry, time, duration));
                    } else {
                        if (lastEndTime != time && lastIsNull) {
                            // add unknown event if between two null states
                            eventList.add(new TimeEvent(entry, lastEndTime, time - lastEndTime));
                        }
                        if (time + duration >= endTime) {
                            // add null event if it intersects the end time
                            eventList.add(new NullTimeEvent(entry, time, duration));
                        }
                    }
                    lastIsNull = true;
                }
                lastEndTime = time + duration;
            }
        } catch (AttributeNotFoundException e) {
            Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
        } catch (TimeRangeException e) {
            Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }
        return eventList;
    }

    /**
     * @since 2.0
     */
    @Override
    protected void synchingToTime(long time) {
        List<TimeGraphEntry> entryList = getEntryList(getTrace());
        if (entryList == null) {
            return;
        }
        for (TimeGraphEntry traceEntry : entryList) {
            for (ITimeGraphEntry threadEntry : traceEntry.getChildren()) {
                ITmfStateSystem ss = ((ThreadEntry) threadEntry).getStateSystem();
                if (ss == null) {
                    continue;
                }
                if (ss.isCancelled()) {
                    continue;
                }
                if (time < ss.getStartTime() || time > ss.getCurrentEndTime()) {
                    continue;
                }
                for (ITimeGraphEntry child : threadEntry.getChildren()) {
                    CallStackEntry callStackEntry = (CallStackEntry) child;
                    try {
                        ITmfStateInterval stackLevelInterval = ss.querySingleState(time, callStackEntry.getQuark());
                        ITmfStateValue nameValue = stackLevelInterval.getStateValue();
                        String name = ""; //$NON-NLS-1$
                        try {
                            if (nameValue.getType() == Type.STRING) {
                                String address = nameValue.unboxStr();
                                name = getFunctionName(address);
                            } else if (nameValue.getType() == Type.INTEGER) {
                                name = "0x" + Integer.toHexString(nameValue.unboxInt()); //$NON-NLS-1$
                            } else if (nameValue.getType() == Type.LONG) {
                                name = "0x" + Long.toHexString(nameValue.unboxLong()); //$NON-NLS-1$
                            }
                        } catch (StateValueTypeException e) {
                        }
                        callStackEntry.setFunctionName(name);
                        if (name.length() > 0) {
                            callStackEntry.setFunctionEntryTime(stackLevelInterval.getStartTime());
                            callStackEntry.setFunctionExitTime(stackLevelInterval.getEndTime() + 1);
                        }
                    } catch (AttributeNotFoundException e) {
                        Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
                    } catch (StateSystemDisposedException e) {
                        /* Ignored */
                    }
                }
            }
        }
        if (Display.getCurrent() != null) {
            getTimeGraphCombo().refresh();
        }
    }

    private void makeActions() {
        fPreviousItemAction = getTimeGraphViewer().getPreviousItemAction();
        fPreviousItemAction.setText(Messages.TmfTimeGraphViewer_PreviousItemActionNameText);
        fPreviousItemAction.setToolTipText(Messages.TmfTimeGraphViewer_PreviousItemActionToolTipText);
        fNextItemAction = getTimeGraphViewer().getNextItemAction();
        fNextItemAction.setText(Messages.TmfTimeGraphViewer_NextItemActionNameText);
        fNextItemAction.setToolTipText(Messages.TmfTimeGraphViewer_NextItemActionToolTipText);
    }

    private void contributeToActionBars() {
        // Create pin action
        contributePinActionToToolBar();
        fPinAction.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (IAction.CHECKED.equals(event.getProperty()) && !isPinned()) {
                    if (fSavedRangeSyncSignal != null) {
                        windowRangeUpdated(fSavedRangeSyncSignal);
                        fSavedRangeSyncSignal = null;
                    }

                    if (fSavedTimeSyncSignal != null) {
                        selectionRangeUpdated(fSavedTimeSyncSignal);
                        fSavedTimeSyncSignal = null;
                    }
                }
            }
        });
    }

    /**
     * @since 2.0
     */
    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        makeActions();
        manager.add(getImportBinaryAction());
        manager.add(getImportMappingAction());
        manager.add(new Separator());
        manager.add(getSortByNameAction());
        manager.add(getSortByIdAction());
        manager.add(getSortByTimeAction());
        manager.add(new Separator());
        manager.add(getTimeGraphCombo().getShowFilterDialogAction());
        manager.add(new Separator());
        manager.add(getTimeGraphViewer().getResetScaleAction());
        manager.add(getPreviousEventAction());
        manager.add(getNextEventAction());
        manager.add(new Separator());
        manager.add(getTimeGraphViewer().getToggleBookmarkAction());
        manager.add(getTimeGraphViewer().getPreviousMarkerAction());
        manager.add(getTimeGraphViewer().getNextMarkerAction());
        manager.add(new Separator());
        manager.add(fPreviousItemAction);
        manager.add(fNextItemAction);
        manager.add(getTimeGraphViewer().getZoomInAction());
        manager.add(getTimeGraphViewer().getZoomOutAction());
    }

    private void createContextMenu() {
        final MenuManager contextMenu = new MenuManager();
        contextMenu.add(getSortByNameAction());
        contextMenu.add(getSortByIdAction());
        contextMenu.add(getSortByTimeAction());

        Tree tree = getTimeGraphCombo().getTreeViewer().getTree();
        Menu menu = contextMenu.createContextMenu(tree);
        tree.setMenu(menu);
    }

    /**
     * Get the the next event action.
     *
     * @return The action object
     */
    private Action getNextEventAction() {
        if (fNextEventAction == null) {
            fNextEventAction = new Action() {
                @Override
                public void run() {
                    TimeGraphViewer viewer = getTimeGraphViewer();
                    ITimeGraphEntry entry = viewer.getSelection();
                    if (entry instanceof CallStackEntry) {
                        try {
                            CallStackEntry callStackEntry = (CallStackEntry) entry;
                            ITmfStateSystem ss = callStackEntry.getStateSystem();
                            long time = Math.max(ss.getStartTime(), Math.min(ss.getCurrentEndTime(), viewer.getSelectionBegin()));
                            ThreadEntry threadEntry = (ThreadEntry) callStackEntry.getParent();
                            int quark = ss.getParentAttributeQuark(callStackEntry.getQuark());
                            ITmfStateInterval stackInterval = ss.querySingleState(time, quark);
                            long newTime = stackInterval.getEndTime() + 1;
                            viewer.setSelectedTimeNotify(newTime, true);
                            stackInterval = ss.querySingleState(Math.min(ss.getCurrentEndTime(), newTime), quark);
                            int stackLevel = stackInterval.getStateValue().unboxInt();
                            ITimeGraphEntry selectedEntry = threadEntry.getChildren().get(Math.max(0, stackLevel - 1));
                            getTimeGraphCombo().setSelection(selectedEntry);
                            viewer.getTimeGraphControl().fireSelectionChanged();
                            startZoomThread(viewer.getTime0(), viewer.getTime1());

                        } catch (AttributeNotFoundException | TimeRangeException | StateSystemDisposedException | StateValueTypeException e) {
                            Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
                        }
                    }
                }
            };

            fNextEventAction.setText(Messages.TmfTimeGraphViewer_NextEventActionNameText);
            fNextEventAction.setToolTipText(Messages.TmfTimeGraphViewer_NextEventActionToolTipText);
            fNextEventAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_NEXT_EVENT));
        }

        return fNextEventAction;
    }

    /**
     * Get the previous event action.
     *
     * @return The Action object
     */
    private Action getPreviousEventAction() {
        if (fPrevEventAction == null) {
            fPrevEventAction = new Action() {
                @Override
                public void run() {
                    TimeGraphViewer viewer = getTimeGraphCombo().getTimeGraphViewer();
                    ITimeGraphEntry entry = viewer.getSelection();
                    if (entry instanceof CallStackEntry) {
                        try {
                            CallStackEntry callStackEntry = (CallStackEntry) entry;
                            ITmfStateSystem ss = callStackEntry.getStateSystem();
                            long time = Math.max(ss.getStartTime(), Math.min(ss.getCurrentEndTime(), viewer.getSelectionBegin()));
                            ThreadEntry threadEntry = (ThreadEntry) callStackEntry.getParent();
                            int quark = ss.getParentAttributeQuark(callStackEntry.getQuark());
                            ITmfStateInterval stackInterval = ss.querySingleState(time, quark);
                            if (stackInterval.getStartTime() == time && time > ss.getStartTime()) {
                                stackInterval = ss.querySingleState(time - 1, quark);
                            }
                            viewer.setSelectedTimeNotify(stackInterval.getStartTime(), true);
                            int stackLevel = stackInterval.getStateValue().unboxInt();
                            ITimeGraphEntry selectedEntry = threadEntry.getChildren().get(Math.max(0, stackLevel - 1));
                            getTimeGraphCombo().setSelection(selectedEntry);
                            viewer.getTimeGraphControl().fireSelectionChanged();
                            startZoomThread(viewer.getTime0(), viewer.getTime1());

                        } catch (AttributeNotFoundException | TimeRangeException | StateSystemDisposedException | StateValueTypeException e) {
                            Activator.getDefault().logError("Error querying state system", e); //$NON-NLS-1$
                        }
                    }
                }
            };

            fPrevEventAction.setText(Messages.TmfTimeGraphViewer_PreviousEventActionNameText);
            fPrevEventAction.setToolTipText(Messages.TmfTimeGraphViewer_PreviousEventActionToolTipText);
            fPrevEventAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_PREV_EVENT));
        }

        return fPrevEventAction;
    }

    private static @Nullable AbstractCallStackAnalysis getCallStackModule(@NonNull ITmfTrace trace) {
        /*
         * Since we cannot know the exact analysis ID (in separate plugins), we
         * will search using the analysis type.
         */
        Iterable<AbstractCallStackAnalysis> modules =
                TmfTraceUtils.getAnalysisModulesOfClass(trace, AbstractCallStackAnalysis.class);
        Iterator<AbstractCallStackAnalysis> it = modules.iterator();
        if (!it.hasNext()) {
            /* This trace does not provide a call-stack analysis */
            return null;
        }

        /*
         * We only look at the first module we find.
         *
         * TODO Handle the advanced case where one trace provides more than one
         * call-stack analysis.
         */
        AbstractCallStackAnalysis module = it.next();
        /* This analysis is not automatic, we need to schedule it on-demand */
        module.schedule();
        module.waitForInitialization();
        return module;
    }

    // ------------------------------------------------------------------------
    // Methods related to function name mapping
    // ------------------------------------------------------------------------

    /**
     * Common code for all import file mapping actions
     */
    private abstract class AbstractImportFileMappingAction extends Action {
        private final String fDialogTitle;

        private AbstractImportFileMappingAction(String dialogTitle) {
            fDialogTitle = dialogTitle;
        }

        @Override
        public void run() {
            FileDialog dialog = new FileDialog(getViewSite().getShell());
            dialog.setText(fDialogTitle);
            final String filePath = dialog.open();
            if (filePath == null) {
                /* No file was selected, don't change anything */
                return;
            }

            /*
             * Start the mapping import in a separate thread (we do not want to
             * UI thread to do this).
             */
            Job job = new Job(Messages.CallStackView_ImportMappingJobName) {
                @Override
                public IStatus run(IProgressMonitor monitor) {
                    fNameMapping = doMapping(new File(filePath));

                    /* Refresh call stack entries and event labels */
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            synchingToTime(getTimeGraphViewer().getSelectionBegin());
                        }
                    });
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        }

        abstract Map<String, String> doMapping(File file);
    }

    /**
     * Toolbar icon to import the function address-to-name mapping file.
     */
    private Action getImportMappingAction() {
        if (fImportMappingAction != null) {
            return fImportMappingAction;
        }
        fImportMappingAction = new AbstractImportFileMappingAction(Messages.CallStackView_ImportMappingDialogTitle) {
            @Override
            Map<String, String> doMapping(File file) {
                return FunctionNameMapper.mapFromNmTextFile(file);
            }
        };

        fImportMappingAction.setText(Messages.CallStackView_ImportMappingButtonText);
        fImportMappingAction.setToolTipText(Messages.CallStackView_ImportMappingButtonTooltip);
        fImportMappingAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(IMPORT_MAPPING_ICON_PATH));

        return fImportMappingAction;
    }

    private Action getSortByNameAction() {
        if (fSortByNameAction == null) {
            fSortByNameAction = new Action(Messages.CallStackView_SortByThreadName, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    if (fSortOption == SortOption.BY_NAME) {
                        saveSortOption(SortOption.BY_NAME_REV);
                    } else {
                        saveSortOption(SortOption.BY_NAME);
                    }
                }
            };
            fSortByNameAction.setToolTipText(Messages.CallStackView_SortByThreadName);
            fSortByNameAction.setImageDescriptor(SORT_BY_NAME_ICON);
        }
        return fSortByNameAction;
    }

    private Action getSortByIdAction() {
        if (fSortByIdAction == null) {
            fSortByIdAction = new Action(Messages.CallStackView_SortByThreadId, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    if (fSortOption == SortOption.BY_ID) {
                        saveSortOption(SortOption.BY_ID_REV);
                    } else {
                        saveSortOption(SortOption.BY_ID);
                    }
                }
            };
            fSortByIdAction.setToolTipText(Messages.CallStackView_SortByThreadId);
            fSortByIdAction.setImageDescriptor(SORT_BY_ID_ICON);
        }
        return fSortByIdAction;
    }

    private Action getSortByTimeAction() {
        if (fSortByTimeAction == null) {
            fSortByTimeAction = new Action(Messages.CallStackView_SortByThreadTime, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    if (fSortOption == SortOption.BY_TIME) {
                        saveSortOption(SortOption.BY_TIME_REV);
                    } else {
                        saveSortOption(SortOption.BY_TIME);
                    }
                }
            };
            fSortByTimeAction.setToolTipText(Messages.CallStackView_SortByThreadTime);
            fSortByTimeAction.setImageDescriptor(SORT_BY_TIME_ICON);
        }
        return fSortByTimeAction;
    }

    private void loadSortOption() {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            return;
        }
        String sortOption = section.get(SORT_OPTION_KEY);
        if (sortOption == null) {
            return;
        }

        // reset defaults
        getSortByNameAction().setChecked(false);
        getSortByNameAction().setImageDescriptor(SORT_BY_NAME_ICON);
        getSortByIdAction().setChecked(false);
        getSortByIdAction().setImageDescriptor(SORT_BY_ID_ICON);
        getSortByTimeAction().setChecked(false);
        getSortByTimeAction().setImageDescriptor(SORT_BY_TIME_ICON);

        if (sortOption.equals(SortOption.BY_NAME.name())) {
            fSortOption = SortOption.BY_NAME;
            fThreadComparator = new ThreadNameComparator(false);
            getSortByNameAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_NAME_REV.name())) {
            fSortOption = SortOption.BY_NAME_REV;
            fThreadComparator = new ThreadNameComparator(true);
            getSortByNameAction().setChecked(true);
            getSortByNameAction().setImageDescriptor(SORT_BY_NAME_REV_ICON);
        } else if (sortOption.equals(SortOption.BY_ID.name())) {
            fSortOption = SortOption.BY_ID;
            fThreadComparator = new ThreadIdComparator(false);
            getSortByIdAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_ID_REV.name())) {
            fSortOption = SortOption.BY_ID_REV;
            fThreadComparator = new ThreadIdComparator(true);
            getSortByIdAction().setChecked(true);
            getSortByIdAction().setImageDescriptor(SORT_BY_ID_REV_ICON);
        } else if (sortOption.equals(SortOption.BY_TIME.name())) {
            fSortOption = SortOption.BY_TIME;
            fThreadComparator = new ThreadTimeComparator(false);
            getSortByTimeAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_TIME_REV.name())) {
            fSortOption = SortOption.BY_TIME_REV;
            fThreadComparator = new ThreadTimeComparator(true);
            getSortByTimeAction().setChecked(true);
            getSortByTimeAction().setImageDescriptor(SORT_BY_TIME_REV_ICON);
        }
    }

    private void saveSortOption(SortOption sortOption) {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }
        section.put(SORT_OPTION_KEY, sortOption.name());
        loadSortOption();
        List<TimeGraphEntry> entryList = getEntryList(getTrace());
        if (entryList == null) {
            return;
        }
        for (TimeGraphEntry traceEntry : entryList) {
            traceEntry.sortChildren(fThreadComparator);
        }
        refresh();
    }

    /**
     * Toolbar icon to import the function address-to-name mapping binary file.
     */
    private Action getImportBinaryAction() {
        if (fImportBinaryFileMappingAction != null) {
            return fImportBinaryFileMappingAction;
        }

        fImportBinaryFileMappingAction = new AbstractImportFileMappingAction(Messages.CallStackView_ImportBinaryFileDialogTitle) {
            @Override
            Map<String, String> doMapping(File file) {
                return FunctionNameMapper.mapFromBinaryFile(file);
            }
        };

        fImportBinaryFileMappingAction.setText(Messages.CallStackView_ImportBinaryFileButtonText);
        fImportBinaryFileMappingAction.setToolTipText(Messages.CallStackView_ImportBinaryFileButtonTooltip);
        fImportBinaryFileMappingAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(IMPORT_BINARY_ICON_PATH));

        return fImportBinaryFileMappingAction;
    }

    String getFunctionName(String address) {
        if (fNameMapping == null) {
            /* No mapping available, just print the addresses */
            return address;
        }
        String ret = fNameMapping.get(address);
        if (ret == null) {
            /*
             * We didn't find this address in the mapping file, just use the
             * address
             */
            return address;
        }
        return ret;
    }

}
