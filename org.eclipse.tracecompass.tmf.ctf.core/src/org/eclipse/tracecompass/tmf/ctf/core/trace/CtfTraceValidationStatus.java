/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ctf.core.trace;

import java.util.Map;

import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;

/**
 * Trace validation status that contains additional information from a CTF
 * trace for further validation.
 *
 * @since 1.0
 */
public class CtfTraceValidationStatus extends TraceValidationStatus {

    private final Map<String, String> fEnvironment;

    /**
     * Constructor
     *
     * @param confidence
     *            the confidence level, 0 is lowest
     * @param pluginId
     *            the unique identifier of the relevant plug-in
     * @param environment
     *            the CTF trace environment variables
     */
    public CtfTraceValidationStatus(int confidence, String pluginId, Map<String, String> environment) {
        super(confidence, pluginId);
        fEnvironment = environment;
    }

    /**
     * Get the CTF trace environment variables
     *
     * @return the CTF trace environment variables
     */
    public Map<String, String> getEnvironment() {
        return fEnvironment;
    }

}
