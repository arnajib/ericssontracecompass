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
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.trace;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Interface for trace types to implement when they can provide additional
 * trace-wide properties.
 *
 * This information will be displayed in the trace's Properties View, among
 * other things.
 *
 * @author Alexandre Montplaisir
 */
@NonNullByDefault
public interface ITmfTraceProperties {

    /**
     * Get the properties related to this trace.
     *
     * @return The map of properties, <name, value>
     */
    public Map<String, String> getTraceProperties();
}
