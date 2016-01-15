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

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.internal.wizards.datatransfer.TarLeveledStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;

/**
 * Various utilities for dealing with archives in the context of importing
 * traces.
 */
@SuppressWarnings({"restriction" })
public class ArchiveUtil {

    /**
     * Returns whether or not the source file is an archive file (Zip, tar,
     * tar.gz, gz).
     *
     * @param sourceFile
     *            the source file
     * @return whether or not the source file is an archive file
     */
    public static boolean isArchiveFile(File sourceFile) {
        String absolutePath = sourceFile.getAbsolutePath();
        return isTarFile(absolutePath) || ArchiveFileManipulations.isZipFile(absolutePath) || isGzipFile(absolutePath);
    }

    private static boolean isTarFile(String fileName) {
        TarFile specifiedTarSourceFile = getSpecifiedTarSourceFile(fileName);
        if (specifiedTarSourceFile != null) {
            try {
                specifiedTarSourceFile.close();
                return true;
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    private static boolean isGzipFile(String fileName) {
        if (!fileName.isEmpty()) {
            try (GzipFile specifiedTarSourceFile = new GzipFile(fileName);) {
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }

    private static ZipFile getSpecifiedZipSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        try {
            return new ZipFile(fileName);
        } catch (ZipException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        return null;
    }

    private static TarFile getSpecifiedTarSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        // FIXME: Work around Bug 463633. Remove this block once we move to Eclipse 4.5.
        if (new File(fileName).length() < 512) {
            return null;
        }

        try {
            return new TarFile(fileName);
        } catch (TarException | IOException e) {
            // ignore
        }

        return null;
    }

    @SuppressWarnings("resource")
    static boolean ensureZipSourceIsValid(String archivePath, Shell shell) {
        ZipFile specifiedFile = getSpecifiedZipSourceFile(archivePath);
        if (specifiedFile == null) {
            return false;
        }
        return ArchiveFileManipulations.closeZipFile(specifiedFile, shell);
    }

    static boolean ensureTarSourceIsValid(String archivePath, Shell shell) {
        TarFile specifiedFile = getSpecifiedTarSourceFile(archivePath);
        if (specifiedFile == null) {
            return false;
        }
        return ArchiveFileManipulations.closeTarFile(specifiedFile, shell);
    }

    static boolean ensureGzipSourceIsValid(String archivePath) {
        return isGzipFile(archivePath);
    }

    /**
     * Get the root file system object and it's associated import provider for
     * the specified source file. A shell is used to display messages in case of
     * errors.
     *
     * @param sourceFile
     *            the source file
     * @param shell
     *            the parent shell to use to display error messages
     * @return the root file system object and it's associated import provider
     */
    @SuppressWarnings("resource")
    public static Pair<IFileSystemObject, FileSystemObjectImportStructureProvider> getRootObjectAndProvider(File sourceFile, Shell shell) {
        if (sourceFile == null) {
            return null;
        }

        IFileSystemObject rootElement = null;
        FileSystemObjectImportStructureProvider importStructureProvider = null;

        // Import from directory
        if (!isArchiveFile(sourceFile)) {
            importStructureProvider = new FileSystemObjectImportStructureProvider(FileSystemStructureProvider.INSTANCE, null);
            rootElement = importStructureProvider.getIFileSystemObject(sourceFile);
        } else {
            // Import from archive
            FileSystemObjectLeveledImportStructureProvider leveledImportStructureProvider = null;
            String archivePath = sourceFile.getAbsolutePath();
            if (isTarFile(archivePath)) {
                if (ensureTarSourceIsValid(archivePath, shell)) {
                    // We close the file when we dispose the import provider,
                    // see disposeSelectionGroupRoot
                    TarFile tarFile = getSpecifiedTarSourceFile(archivePath);
                    leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new TarLeveledStructureProvider(tarFile), archivePath);
                }
            } else if (ensureZipSourceIsValid(archivePath, shell)) {
                // We close the file when we dispose the import provider, see
                // disposeSelectionGroupRoot
                ZipFile zipFile = getSpecifiedZipSourceFile(archivePath);
                leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new ZipLeveledStructureProvider(zipFile), archivePath);
            } else if (ensureGzipSourceIsValid(archivePath)) {
                // We close the file when we dispose the import provider, see
                // disposeSelectionGroupRoot
                GzipFile zipFile = null;
                try {
                    zipFile = new GzipFile(archivePath);
                    leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new GzipLeveledStructureProvider(zipFile), archivePath);
                } catch (IOException e) {
                    // do nothing
                }
            }
            if (leveledImportStructureProvider == null) {
                return null;
            }
            rootElement = leveledImportStructureProvider.getRoot();
            importStructureProvider = leveledImportStructureProvider;
        }

        if (rootElement == null) {
            return null;
        }

        return new Pair<>(rootElement, importStructureProvider);
    }
}
