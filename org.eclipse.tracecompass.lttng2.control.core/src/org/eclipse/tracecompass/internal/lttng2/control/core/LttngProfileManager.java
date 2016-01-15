/**********************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.tracecompass.internal.lttng2.control.core;

import java.io.File;

import org.eclipse.core.runtime.IPath;

/**
 * Class to manage LTTng profiles files in workspace.
 *
 * @author Bernd Hufmann
 */
public class LttngProfileManager {

    private static final String FOLDER = "sessions"; //$NON-NLS-1$

    private static final IPath SAVED_PROFILE_PATH =
        Activator.getDefault().getStateLocation().append(FOLDER);

    static {
        File dir = SAVED_PROFILE_PATH.toFile();
        /* Check if directory exists, otherwise create it */
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
    }

    /**
     * Returns all LTTng profile files available in workspace.
     *
     * @return array with LTTng profiles
     */
    public static File[] getProfiles() {
        return SAVED_PROFILE_PATH.toFile().listFiles();
    }

    /**
     * Gets the path where the profiles are located in the workspace.
     *
     * @return the profile path
     */
    public static IPath getProfilePath() {
        return SAVED_PROFILE_PATH;
    }
}
