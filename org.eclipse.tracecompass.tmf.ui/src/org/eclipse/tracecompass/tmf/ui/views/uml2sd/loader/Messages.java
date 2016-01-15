/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bernd Hufmann - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views.uml2sd.loader;

import org.eclipse.osgi.util.NLS;

/**
 * Messages related to the sequence diagram loader
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.tmf.ui.views.uml2sd.loader.messages"; //$NON-NLS-1$
    public static String TmfUml2SDSyncLoader_ViewName;
    public static String TmfUml2SDSyncLoader_CategoryLifeline;
    public static String TmfUml2SDSyncLoader_CategoryMessage;
    public static String TmfUml2SDSyncLoader_FrameName;
    public static String TmfUml2SDSyncLoader_SearchJobDescrition;
    public static String TmfUml2SDSyncLoader_SearchNotFound;

    public static String TmfUml2SDSyncLoader_EventTypeSend;
    public static String TmfUml2SDSyncLoader_EventTypeReceive;
    public static String TmfUml2SDSyncLoader_FieldSender;
    public static String TmfUml2SDSyncLoader_FieldReceiver;
    public static String TmfUml2SDSyncLoader_FieldSignal;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
