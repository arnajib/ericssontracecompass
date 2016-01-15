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

package org.eclipse.tracecompass.statesystem.core.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.tracecompass.internal.statesystem.core.AttributeTree;
import org.eclipse.tracecompass.internal.statesystem.core.StateSystem;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.junit.Test;

/**
 * Test the {@link AttributeTree} class
 *
 * @author Patrick Tasse
 */
public class AttributeTreeTest {

    private static final String THREADS = "Threads";
    private static final String[] NAMES = {
        "",      // ''
        "\0",    // 'Null'
        "a",     // 'a'
        "\"",    // '"'
        "/",     // '/'
        "\\",    // '\'
        "ab",    // 'ab'
        "\"\"",  // '""'
        "a/",    // 'a/'
        "a\\",   // 'a\'
        "/a",    // '/a'
        "//",    // '//'
        "/\\",   // '/\'
        "\\a",   // '\a'
        "\\/",   // '\/'
        "\\\\",  // '\\'
        "abc",   // 'abc'
        "\"/\"", // '"/"'
        "ab/",   // 'ab/'
        "ab\\",  // 'ab\'
        "a/b",   // 'a/b'
        "a//",   // 'a//'
        "a/\\",  // 'a/\'
        "a\\b",  // 'a\b'
        "a\\/",  // 'a\/'
        "a\\\\", // 'a\\'
        "/ab",   // '/ab'
        "/a/",   // '/a/'
        "/a\\",  // '/a\'
        "//a",   // '//a'
        "///",   // '///'
        "//\\",  // '//\'
        "/\\a",  // '/\a'
        "/\\/",  // '/\/'
        "/\\\\", // '/\\'
        "\\ab",  // '\ab'
        "\\a/",  // '\a/'
        "\\a\\", // '\a\'
        "\\/a",  // '\/a'
        "\\//",  // '\//'
        "\\/\\", // '\/\'
        "\\\\a", // '\\a'
        "\\\\/", // '\\/'
        "\\\\\\" // '\\\'
    };
    private static final String STATUS = "Status";

    /**
     * Test attribute tree file storage.
     * <p>
     * Tests that an attribute tree written to file is read back correctly.
     * <p>
     * Tests {@link AttributeTree#writeSelf(File, long)} and
     * {@link AttributeTree#AttributeTree(StateSystem, FileInputStream)}.
     *
     * @throws IOException
     *             if there is an error accessing the test file
     * @throws AttributeNotFoundException
     *             if the test fails
     */
    @Test
    public void testAttributeTreeFileStorage() throws IOException, AttributeNotFoundException {
        File file = File.createTempFile("AttributeTreeTest", ".ht");
        IStateHistoryBackend backend1 = StateHistoryBackendFactory.createNullBackend("test");
        StateSystem ss1 = new StateSystem(backend1);
        AttributeTree attributeTree1 = new AttributeTree(ss1);
        for (String name : NAMES) {
            String[] path = new String[] { THREADS, name, STATUS };
            attributeTree1.getQuarkAndAdd(-1, path);
        }
        attributeTree1.writeSelf(file, 0L);
        ss1.dispose();

        IStateHistoryBackend backend2 = StateHistoryBackendFactory.createNullBackend("test");
        StateSystem ss2 = new StateSystem(backend2);
        try (FileInputStream fis = new FileInputStream(file)) {
            AttributeTree attributeTree2 = new AttributeTree(ss2, fis);
            for (String name : NAMES) {
                String[] path = new String[] { THREADS, name, STATUS };
                int quark = attributeTree2.getQuarkDontAdd(-1, path);
                assertArrayEquals(path, attributeTree2.getFullAttributePathArray(quark));
                assertEquals(name, attributeTree2.getAttributeName(attributeTree2.getParentAttributeQuark(quark)));
            }
        } finally {
            ss2.dispose();
            file.delete();
        }
    }
}
