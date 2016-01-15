/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public final class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.ctf.core.event.metadata.messages"; //$NON-NLS-1$

    public static String IOStructGen_UnknownTraceAttributeWarning;
    public static String IOStructGen_UnknownStreamAttributeWarning;
    public static String IOStructGen_UnknownIntegerAttributeWarning;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
