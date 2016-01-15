/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace;

/**
 * This interface abstracts the differences between different kinds of
 * FileSystemObjects such as File, TarEntry, ZipEntry, etc. This allows clients
 * (TraceFileSystemElement, TraceValidateAndImportOperation) to handle all the
 * types transparently.
 */
public interface IFileSystemObject {

    /**
     * Get the name of the file system object (last segment).
     *
     * @return the name of the file system object.
     */
    String getName();

    /**
     * Get the absolute path of the file system object.
     *
     * @return the absolute path of the file system object
     */
    String getAbsolutePath();

    /**
     * Get the source location for this file system object.
     *
     * @return the source location
     */
    String getSourceLocation();

    /**
     * Returns the raw object wrapped by this IFileSystemObject (File, TarEntry, etc).
     *
     * @return the raw object wrapped by this IFileSystemObject
     */
    Object getRawFileSystemObject();

    /**
     * Returns whether or not the file system object exists.
     *
     * @return whether or not the file system object exists
     */
    boolean exists();
}