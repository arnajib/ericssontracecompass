/*****************************************************************************
 * Copyright (c) 2007, 2014 Intel Corporation, Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Intel Corporation - Initial API and implementation
 *   Vitaly A. Provodin, Intel - Initial API and implementation
 *   Alvaro Sanchez-Leon - Updated for TMF
 *   Patrick Tasse - Refactoring
 *****************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.Resolution;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;

/**
 * Handler for the tool tips in the generic time graph view.
 *
 * @version 1.0
 * @author Alvaro Sanchez-Leon
 * @author Patrick Tasse
 */
public class TimeGraphTooltipHandler {

    private static final String MIN_STRING = "< 0.01%"; //$NON-NLS-1$

    private static final double MIN_RATIO = 0.0001;

    private static final String MAX_STRING = "> 1000%"; //$NON-NLS-1$

    private static final int MAX_RATIO = 10;

    private static final int OFFSET = 16;

    private Shell fTipShell;
    private Composite fTipComposite;
    private ITimeDataProvider fTimeDataProvider;
    private ITimeGraphPresentationProvider fTimeGraphProvider = null;

    /**
     * Standard constructor
     *
     * @param graphProv
     *            The presentation provider
     * @param timeProv
     *            The time provider
     */
    public TimeGraphTooltipHandler(ITimeGraphPresentationProvider graphProv,
            ITimeDataProvider timeProv) {

        this.fTimeGraphProvider = graphProv;
        this.fTimeDataProvider = timeProv;
    }

    /**
     * Set the time data provider
     *
     * @param timeDataProvider
     *            The time data provider
     */
    public void setTimeProvider(ITimeDataProvider timeDataProvider) {
        fTimeDataProvider = timeDataProvider;
    }

    private void createTooltipShell(Shell parent) {
        final Display display = parent.getDisplay();
        if (fTipShell != null && !fTipShell.isDisposed()) {
            fTipShell.dispose();
        }
        fTipShell = new Shell(parent, SWT.ON_TOP | SWT.TOOL);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridLayout.marginWidth = 2;
        gridLayout.marginHeight = 2;
        fTipShell.setLayout(gridLayout);
        fTipShell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        fTipComposite = new Composite(fTipShell, SWT.NONE);
        fTipComposite.setLayout(new GridLayout(3, false));
        setupControl(fTipComposite);

    }

    /**
     * Callback for the mouse-over tooltip
     *
     * @param control
     *            The control object to use
     */
    public void activateHoverHelp(final Control control) {
        control.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if (fTipShell != null && !fTipShell.isDisposed()) {
                    fTipShell.dispose();
                }
            }
        });

        control.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e) {
                if (fTipShell != null && !fTipShell.isDisposed()) {
                    fTipShell.dispose();
                }
            }
        });

        control.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                if (fTipShell != null && !fTipShell.isDisposed()) {
                    Point pt = control.toDisplay(e.x, e.y);
                    if (!fTipShell.getBounds().contains(pt)) {
                        fTipShell.dispose();
                    }
                }
            }

            private void addItem(String name, String value) {
                Label nameLabel = new Label(fTipComposite, SWT.NO_FOCUS);
                nameLabel.setText(name);
                setupControl(nameLabel);
                Label separator = new Label(fTipComposite, SWT.NO_FOCUS | SWT.SEPARATOR | SWT.VERTICAL);
                GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
                gd.heightHint = nameLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
                separator.setLayoutData(gd);
                setupControl(separator);
                Label valueLabel = new Label(fTipComposite, SWT.NO_FOCUS);
                valueLabel.setText(value);
                setupControl(valueLabel);
            }

            private void fillValues(Point pt, TimeGraphControl timeGraphControl, ITimeGraphEntry entry) {
                if (entry == null) {
                    return;
                }
                if (entry.hasTimeEvents()) {
                    long currPixelTime = timeGraphControl.getTimeAtX(pt.x);
                    long nextPixelTime = timeGraphControl.getTimeAtX(pt.x + 1);
                    if (nextPixelTime == currPixelTime) {
                        nextPixelTime++;
                    }
                    ITimeEvent currEvent = Utils.findEvent(entry, currPixelTime, 0);
                    ITimeEvent nextEvent = Utils.findEvent(entry, currPixelTime, 1);

                    /*
                     * if there is no current event at the start of the current
                     * pixel range, or if the current event starts before the
                     * current pixel range, use the next event as long as it
                     * starts within the current pixel range
                     */
                    if ((currEvent == null || currEvent.getTime() < currPixelTime) &&
                            (nextEvent != null && nextEvent.getTime() < nextPixelTime)) {
                        currEvent = nextEvent;
                        currPixelTime = nextEvent.getTime();
                    }

                    // state name
                    String stateTypeName = fTimeGraphProvider.getStateTypeName(entry);
                    String entryName = entry.getName();
                    if (stateTypeName == null) {
                        stateTypeName = fTimeGraphProvider.getStateTypeName();
                    }

                    if (!entryName.isEmpty()) {
                        addItem(stateTypeName, entry.getName());
                    }

                    if (currEvent == null || currEvent instanceof NullTimeEvent) {
                        return;
                    }

                    // state
                    String state = fTimeGraphProvider.getEventName(currEvent);
                    if (state != null) {
                        addItem(Messages.TmfTimeTipHandler_TRACE_STATE, state);
                    }

                    // This block receives a list of <String, String> values to
                    // be added to the tip table
                    Map<String, String> eventAddOns = fTimeGraphProvider.getEventHoverToolTipInfo(currEvent, currPixelTime);
                    if (eventAddOns != null) {
                        for (Entry<String, String> eventAddOn : eventAddOns.entrySet()) {
                            addItem(eventAddOn.getKey(), eventAddOn.getValue());
                        }
                    }
                    if (fTimeGraphProvider.displayTimesInTooltip()) {
                        long eventStartTime = -1;
                        long eventDuration = -1;
                        long eventEndTime = -1;

                        eventStartTime = currEvent.getTime();
                        eventDuration = currEvent.getDuration();
                        if (eventDuration < 0 && nextEvent != null) {
                            eventEndTime = nextEvent.getTime();
                            eventDuration = eventEndTime - eventStartTime;
                        } else {
                            eventEndTime = eventStartTime + eventDuration;
                        }

                        Resolution res = Resolution.NANOSEC;
                        TimeFormat tf = fTimeDataProvider.getTimeFormat();
                        String startTime = "?"; //$NON-NLS-1$
                        String duration = "?"; //$NON-NLS-1$
                        String endTime = "?"; //$NON-NLS-1$
                        if (fTimeDataProvider instanceof ITimeDataProviderConverter) {
                            ITimeDataProviderConverter tdp = (ITimeDataProviderConverter) fTimeDataProvider;
                            if (eventStartTime > -1) {
                                eventStartTime = tdp.convertTime(eventStartTime);
                                startTime = Utils.formatTime(eventStartTime, tf, res);
                            }
                            if (eventEndTime > -1) {
                                eventEndTime = tdp.convertTime(eventEndTime);
                                endTime = Utils.formatTime(eventEndTime, tf, res);
                            }
                            if (eventDuration > -1) {
                                duration = Utils.formatDelta(eventEndTime - eventStartTime, tf, res);
                            }
                        } else {
                            if (eventStartTime > -1) {
                                startTime = Utils.formatTime(eventStartTime, tf, res);
                            }
                            if (eventEndTime > -1) {
                                endTime = Utils.formatTime(eventEndTime, tf, res);
                            }
                            if (eventDuration > -1) {
                                duration = Utils.formatDelta(eventDuration, tf, res);
                            }
                        }
                        if (tf == TimeFormat.CALENDAR) {
                            addItem(Messages.TmfTimeTipHandler_TRACE_DATE,
                                    eventStartTime > -1 ? Utils.formatDate(eventStartTime) : "?"); //$NON-NLS-1$
                        }
                        if (eventDuration > 0) {
                            addItem(Messages.TmfTimeTipHandler_TRACE_START_TIME, startTime);
                            addItem(Messages.TmfTimeTipHandler_TRACE_STOP_TIME, endTime);
                        } else {
                            addItem(Messages.TmfTimeTipHandler_TRACE_EVENT_TIME, startTime);
                        }

                        if (eventDuration > 0) {
                            addItem(Messages.TmfTimeTipHandler_DURATION, duration);
                            long begin = fTimeDataProvider.getSelectionBegin();
                            long end = fTimeDataProvider.getSelectionEnd();
                            final long delta = Math.abs(end - begin);
                            final double durationRatio = (double) eventDuration / (double) delta;
                            if (delta > 0) {
                                String percentage;
                                if (durationRatio > MAX_RATIO) {
                                    percentage = MAX_STRING;
                                } else if (durationRatio < MIN_RATIO) {
                                    percentage = MIN_STRING;
                                } else {
                                    percentage = String.format("%,.2f%%", durationRatio * 100.0); //$NON-NLS-1$
                                }

                                addItem(Messages.TmfTimeTipHandler_PERCENT_OF_SELECTION, percentage);
                            }
                        }
                    }
                }
            }

            private void fillValues(ILinkEvent linkEvent) {
                addItem(Messages.TmfTimeTipHandler_LINK_SOURCE, linkEvent.getEntry().getName());
                addItem(Messages.TmfTimeTipHandler_LINK_TARGET, linkEvent.getDestinationEntry().getName());

                // This block receives a list of <String, String> values to be
                // added to the tip table
                Map<String, String> eventAddOns = fTimeGraphProvider.getEventHoverToolTipInfo(linkEvent);
                if (eventAddOns != null) {
                    for (Entry<String, String> eventAddOn : eventAddOns.entrySet()) {
                        addItem(eventAddOn.getKey(), eventAddOn.getValue());
                    }
                }
                if (fTimeGraphProvider.displayTimesInTooltip()) {
                    long sourceTime = linkEvent.getTime();
                    long duration = linkEvent.getDuration();
                    long targetTime = sourceTime + duration;
                    if (fTimeDataProvider instanceof ITimeDataProviderConverter) {
                        ITimeDataProviderConverter tdp = (ITimeDataProviderConverter) fTimeDataProvider;
                        sourceTime = tdp.convertTime(sourceTime);
                        targetTime = tdp.convertTime(targetTime);
                        duration = targetTime - sourceTime;
                    }
                    Resolution res = Resolution.NANOSEC;
                    TimeFormat tf = fTimeDataProvider.getTimeFormat();
                    if (tf == TimeFormat.CALENDAR) {
                        addItem(Messages.TmfTimeTipHandler_TRACE_DATE, Utils.formatDate(sourceTime));
                    }
                    if (duration > 0) {
                        addItem(Messages.TmfTimeTipHandler_LINK_SOURCE_TIME, Utils.formatTime(sourceTime, tf, res));
                        addItem(Messages.TmfTimeTipHandler_LINK_TARGET_TIME, Utils.formatTime(targetTime, tf, res));
                        addItem(Messages.TmfTimeTipHandler_DURATION, Utils.formatDelta(duration, tf, res));
                    } else {
                        addItem(Messages.TmfTimeTipHandler_LINK_TIME, Utils.formatTime(sourceTime, tf, res));
                    }
                }
            }

            @Override
            public void mouseHover(MouseEvent event) {
                if ((event.stateMask & SWT.BUTTON_MASK) != 0) {
                    return;
                }
                Point pt = new Point(event.x, event.y);
                TimeGraphControl timeGraphControl = (TimeGraphControl) event.widget;
                createTooltipShell(timeGraphControl.getShell());
                for (Control child : fTipComposite.getChildren()) {
                    child.dispose();
                }
                if ((event.stateMask & SWT.MODIFIER_MASK) != SWT.SHIFT) {
                    ILinkEvent linkEvent = timeGraphControl.getArrow(pt);
                    if (linkEvent != null) {
                        fillValues(linkEvent);
                    }
                }
                if (fTipComposite.getChildren().length == 0) {
                    ITimeGraphEntry entry = timeGraphControl.getEntry(pt);
                    fillValues(pt, timeGraphControl, entry);
                }
                if (fTipComposite.getChildren().length == 0) {
                    return;
                }
                fTipShell.pack();
                Point tipPosition = control.toDisplay(pt);
                fTipShell.pack();
                setHoverLocation(fTipShell, tipPosition);
                fTipShell.setVisible(true);
            }
        });
    }

    private static void setHoverLocation(Shell shell, Point position) {
        Rectangle displayBounds = shell.getDisplay().getBounds();
        Rectangle shellBounds = shell.getBounds();
        if (position.x + shellBounds.width + OFFSET > displayBounds.width && position.x - shellBounds.width - OFFSET >= 0) {
            shellBounds.x = position.x - shellBounds.width - OFFSET;
        } else {
            shellBounds.x = Math.max(Math.min(position.x + OFFSET, displayBounds.width - shellBounds.width), 0);
        }
        if (position.y + shellBounds.height + OFFSET > displayBounds.height && position.y - shellBounds.height - OFFSET >= 0) {
            shellBounds.y = position.y - shellBounds.height - OFFSET;
        } else {
            shellBounds.y = Math.max(Math.min(position.y + OFFSET, displayBounds.height - shellBounds.height), 0);
        }
        shell.setBounds(shellBounds);
    }

    private void setupControl(Control control) {
        control.setForeground(fTipShell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        control.setBackground(fTipShell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        control.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                fTipShell.dispose();
            }
        });

        control.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseExit(MouseEvent e) {
                fTipShell.dispose();
            }
        });

        control.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e) {
                fTipShell.dispose();
            }
        });
    }
}
