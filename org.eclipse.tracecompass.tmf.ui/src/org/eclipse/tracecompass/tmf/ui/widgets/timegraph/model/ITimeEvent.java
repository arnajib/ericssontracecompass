/*******************************************************************************
 * Copyright (c) 2009, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alvaro Sanchez-Leon - Initial API and implementation
 *   Patrick Tasse - Refactoring
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model;

/**
 * Interface for time events, for use in the timegraph view
 *
 * @version 1.0
 * @author Alvaro Sanchez-Leon
 * @author Patrick Tasse
 */
public interface ITimeEvent {

    /**
     * Get the entry matching this time event.
     *
     * @return The time graph entry
     */
    ITimeGraphEntry getEntry();

    /**
     * Get the timestamp of this event.
     *
     * @return The event's time
     */
    long getTime();

    /**
     * @return
     * <list>
     * <li>-1: Considers duration to be from current event till the next</li>
     * <li>0: Duration is not relevant e.g. a Burst / no state associated</li>
     * <li>>0: Valid duration value specified</li>
     * </list>
     * <p>
     */
    long getDuration();

    /**
     * Split an event in two at the specified time and keep the part before the
     * split. If the time is smaller or equal to the event's start, the returned
     * event is null.
     *
     * @param splitTime
     *            the time at which the event is to be split
     * @return The part before the split time
     */
    ITimeEvent splitBefore(long splitTime);

    /**
     * Split an event in two at the specified time and keep the part after the
     * split. If the time is greater or equal to the event's end, the returned
     * event is null.
     *
     * @param splitTime
     *            the time at which the event is to be split
     * @return The part after the split time
     */
    ITimeEvent splitAfter(long splitTime);
}