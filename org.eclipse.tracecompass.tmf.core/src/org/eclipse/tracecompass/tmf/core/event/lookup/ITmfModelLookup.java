/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.tmf.core.event.lookup;


/**
 * Interface for events to implement to provide information for model element lookup.
 *
 * @author Bernd Hufmann
 */
public interface ITmfModelLookup {
    /**
     * Returns a model URI string.
     *
     * @return a model URI string.
     */
    public String getModelUri();
}
