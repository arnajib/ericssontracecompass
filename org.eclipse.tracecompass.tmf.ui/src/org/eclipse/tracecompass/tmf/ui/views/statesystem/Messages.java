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

package org.eclipse.tracecompass.tmf.ui.views.statesystem;

import org.eclipse.osgi.util.NLS;

/**
 * Localizable strings in the State System Visualizer.
 *
 * @author Alexandre Montplaisir
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.tmf.ui.views.statesystem.messages"; //$NON-NLS-1$

    /**
     * Initializer
     */
    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    /**
     * Private constructor (static class)
     */
    private Messages() {}

    /** Label for the first column */
    public static String TreeNodeColumnLabel;

    /** Label for the "quark" column" */
    public static String QuarkColumnLabel;

    /** Label for the "value" column */
    public static String ValueColumnLabel;

    /** Label for the "type" column */
    public static String TypeColumnLabel;

    /** Label for the "start time" column */
    public static String StartTimeColumLabel;

    /** Label for the "end time" column */
    public static String EndTimeColumLabel;

    /** Label for the "attribute path" column */
    public static String AttributePathColumnLabel;

    /**
     * Printing "out of range" in the value column when the current timestamp is
     * outside of the SS's range.
     */
    public static String OutOfRangeMsg;

    /** Label for the Filter button */
    public static String FilterButton;

    /** Label for the type Interger */
    public static String TypeInteger;

    /** Label for the type Long */
    public static String TypeLong;

    /** Label for type Double */
    public static String TypeDouble;

    /** Label for the type String */
    public static String TypeString;
}
