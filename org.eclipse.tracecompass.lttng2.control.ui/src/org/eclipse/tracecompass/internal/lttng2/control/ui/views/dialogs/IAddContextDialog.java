/**********************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.tracecompass.internal.lttng2.control.ui.views.dialogs;

import java.util.List;

/**
 * <p>
 * Interface for providing information about contexts to be added to channels/events.
 * </p>
 *
 * @author Bernd Hufmann
 */
public interface IAddContextDialog {

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Sets the available contexts to choose from.
     * @param contexts - a list of available contexts.
     */
    void setAvalibleContexts(List<String> contexts);

    /**
     * @return array of contexts to be added
     */
    List<String> getContexts();

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------
    /**
     * @return returns the open return value
     */
    int open();
}
