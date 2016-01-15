/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.core.module;

import org.eclipse.osgi.util.NLS;

/**
 * Externalized messages for the XML analysis module package
 *
 * @author Geneviève Bastien
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.tmf.analysis.xml.core.module.messages"; //$NON-NLS-1$

    /** Error copying XML file to workspace folder */
    public static String XmlUtils_ErrorCopyingFile;
    /** XML parse error */
    public static String XmlUtils_XmlParseError;
    /** Error occurred while validating XML */
    public static String XmlUtils_XmlValidateError;
    /** XML validation error */
    public static String XmlUtils_XmlValidationError;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
