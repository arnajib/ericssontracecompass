/*******************************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.event;

import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;

/**
 * The generic lost event structure in TMF.
 *
 * In some demanding situations, tracers can be overwhelmed and have a hard time
 * keeping up with the flow of events to record. Usually, even if a tracer can't
 * keep up, it can at least record the number of events that it lost.
 *
 * This interface provides the different components (e.g. views) with a mean to
 * identify and highlight such events.
 *
 * This interface extends ITmfEvent by adding the number of lost events for a
 * 'problematic' time range.
 *
 * @see TmfLostEvent
 *
 * @author Francois Chouinard
 */
public interface ITmfLostEvent extends ITmfEvent {

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    /**
     * @return the 'problem' time range
     */
    TmfTimeRange getTimeRange();

    /**
     * @return the number of lost events in the time range
     */
    long getNbLostEvents();

}
