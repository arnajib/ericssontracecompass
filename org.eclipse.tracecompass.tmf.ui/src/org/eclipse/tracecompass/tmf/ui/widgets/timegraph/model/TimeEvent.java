/*******************************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Geneviève Bastien - Added the fValue parameter to avoid subclassing
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model;

/**
 * Generic TimeEvent implementation
 *
 * @version 1.0
 * @author Patrick Tasse
 */
public class TimeEvent implements ITimeEvent {

    /** TimeGraphEntry matching this time event */
    protected ITimeGraphEntry fEntry;

    /** Beginning timestamp of this time event */
    protected long fTime;

    /** Duration of this time event */
    protected long fDuration;

    private final int fValue;

    /**
     * Default value when no other value present
     */
    private static final int NOVALUE = Integer.MIN_VALUE;

    /**
     * Standard constructor
     *
     * @param entry
     *            The entry matching this event
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of the event
     */
    public TimeEvent(ITimeGraphEntry entry, long time, long duration) {
        this(entry, time, duration, NOVALUE);

    }

    /**
     * Constructor
     *
     * @param entry
     *            The entry to which this time event is assigned
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of this event
     * @param value
     *            The status assigned to the event
     */
    public TimeEvent(ITimeGraphEntry entry, long time, long duration,
            int value) {
        fEntry = entry;
        fTime = time;
        fDuration = duration;
        fValue = value;
    }

    /**
     * Get this event's status
     *
     * @return The integer matching this status
     */
    public int getValue() {
        return fValue;
    }

    /**
     * Return whether an event has a value
     *
     * @return true if the event has a value
     */
    public boolean hasValue() {
        return (fValue != NOVALUE);
    }

    @Override
    public ITimeGraphEntry getEntry() {
        return fEntry;
    }

    @Override
    public long getTime() {
        return fTime;
    }

    @Override
    public long getDuration() {
        return fDuration;
    }

    @Override
    public ITimeEvent splitBefore(long splitTime) {
        return (splitTime > fTime ?
                new TimeEvent(fEntry, fTime, Math.min(fDuration, splitTime - fTime), fValue) :
                null);
    }

    @Override
    public ITimeEvent splitAfter(long splitTime) {
        return (splitTime < fTime + fDuration ?
                new TimeEvent(fEntry, Math.max(fTime, splitTime), fDuration - Math.max(0, splitTime - fTime),
                        fValue) :
                null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " start=" + fTime + " end=" + (fTime + fDuration) + " duration=" + fDuration + (hasValue() ? (" value=" + fValue) : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }
}
