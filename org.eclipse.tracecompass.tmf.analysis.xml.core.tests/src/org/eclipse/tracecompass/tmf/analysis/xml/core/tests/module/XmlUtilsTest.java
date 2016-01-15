/*******************************************************************************
 * Copyright (c) 2014, 2015 École Polytechnique de Montréal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.core.tests.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.XmlStateSystemModule;
import org.eclipse.tracecompass.tmf.analysis.xml.core.tests.Activator;
import org.eclipse.tracecompass.tmf.analysis.xml.core.tests.common.TmfXmlTestFiles;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests for the {@link XmlUtils} class
 *
 * @author Geneviève Bastien
 */
public class XmlUtilsTest {

    private static final Path PATH_INVALID = new Path("test_xml_files/test_invalid");
    private static final Path PATH_VALID = new Path("test_xml_files/test_valid");

    /**
     * Empty the XML directory after the test
     */
    @After
    public void emptyXmlFolder() {
        File fFolder = XmlUtils.getXmlFilesPath().toFile();
        if (!(fFolder.isDirectory() && fFolder.exists())) {
            return;
        }
        for (File xmlFile : fFolder.listFiles()) {
            xmlFile.delete();
        }
    }

    /**
     * Test the {@link XmlUtils#getXmlFilesPath()} method
     */
    @Test
    public void testXmlPath() {
        IPath xmlPath = XmlUtils.getXmlFilesPath();

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath workspacePath = workspace.getRoot().getRawLocation();
        workspacePath = workspacePath.addTrailingSeparator()
                .append(".metadata").addTrailingSeparator().append(".plugins")
                .addTrailingSeparator()
                .append("org.eclipse.tracecompass.tmf.analysis.xml.core")
                .addTrailingSeparator().append("xml_files");

        assertEquals(xmlPath, workspacePath);
    }

    /**
     * test the {@link XmlUtils#xmlValidate(File)} method
     */
    @Test
    public void testXmlValidate() {
        File testXmlFile = TmfXmlTestFiles.VALID_FILE.getFile();
        if ((testXmlFile == null) || !testXmlFile.exists()) {
            fail("XML test file does not exist");
        }
        IStatus status = XmlUtils.xmlValidate(testXmlFile);
        if (!status.isOK()) {
            fail(status.getMessage());
        }

        testXmlFile = TmfXmlTestFiles.INVALID_FILE.getFile();
        if ((testXmlFile == null) || !testXmlFile.exists()) {
            fail("XML test file does not exist");
        }
        assertFalse(XmlUtils.xmlValidate(testXmlFile).isOK());

    }

    /**
     * Test various invalid files and make sure they are invalid
     */
    @Test
    public void testXmlValidateInvalid() {
        IPath path = Activator.getAbsolutePath(PATH_INVALID);
        File file = path.toFile();

        File[] invalidFiles = file.listFiles();
        assertTrue(invalidFiles.length > 0);
        for (File f : invalidFiles) {
            assertFalse("File " + f.getName(), XmlUtils.xmlValidate(f).isOK());
        }
    }

    /**
     * Test various valid files and make sure they are valid
     */
    @Test
    public void testXmlValidateValid() {
        IPath path = Activator.getAbsolutePath(PATH_VALID);
        File file = path.toFile();

        File[] validFiles = file.listFiles();
        assertTrue(validFiles.length > 0);
        for (File f : validFiles) {
            assertTrue("File " + f.getName(), XmlUtils.xmlValidate(f).isOK());
        }
    }

    /**
     * test the {@link XmlUtils#addXmlFile(File)} method
     */
    @Test
    public void testXmlAddFile() {
        /* Check the file does not exist */
        IPath xmlPath = XmlUtils.getXmlFilesPath().addTrailingSeparator().append("test_valid.xml");
        File destFile = xmlPath.toFile();
        assertFalse(destFile.exists());

        /* Add test_valid.xml file */
        File testXmlFile = TmfXmlTestFiles.VALID_FILE.getFile();
        if ((testXmlFile == null) || !testXmlFile.exists()) {
            fail("XML test file does not exist");
        }

        XmlUtils.addXmlFile(testXmlFile);
        assertTrue(destFile.exists());
    }

    private static final @NonNull String ANALYSIS_ID = "kernel.linux.sp";

    /**
     * Test the {@link XmlUtils#getElementInFile(String, String, String)} method
     */
    @Test
    public void testGetElementInFile() {
        File testXmlFile = TmfXmlTestFiles.VALID_FILE.getFile();
        if ((testXmlFile == null) || !testXmlFile.exists()) {
            fail("XML test file does not exist");
        }
        /*
         * This sounds useless, but I get a potential null pointer warning
         * otherwise
         */
        if (testXmlFile == null) {
            return;
        }

        Element analysis = XmlUtils.getElementInFile(testXmlFile.getAbsolutePath(), TmfXmlStrings.STATE_PROVIDER, ANALYSIS_ID);
        assertNotNull(analysis);
    }

    /**
     * Test the {@link XmlUtils#getChildElements(Element)} and
     * {@link XmlUtils#getChildElements(Element, String)} methods
     */
    @Test
    public void testGetChildElements() {
        File testXmlFile = TmfXmlTestFiles.VALID_FILE.getFile();
        if ((testXmlFile == null) || !testXmlFile.exists()) {
            fail("XML test file does not exist");
        }
        /*
         * This sounds useless, but I get a potential null pointer warning
         * otherwise
         */
        if (testXmlFile == null) {
            return;
        }

        Element analysis = XmlUtils.getElementInFile(testXmlFile.getAbsolutePath(), TmfXmlStrings.STATE_PROVIDER, ANALYSIS_ID);

        List<Element> values = XmlUtils.getChildElements(analysis, TmfXmlStrings.LOCATION);
        assertEquals(5, values.size());

        Element aLocation = values.get(0);
        List<Element> attributes = XmlUtils.getChildElements(aLocation, TmfXmlStrings.STATE_ATTRIBUTE);
        assertEquals(2, attributes.size());

        values = XmlUtils.getChildElements(analysis, TmfXmlStrings.HEAD);
        assertEquals(1, values.size());

        Element head = values.get(0);
        values = XmlUtils.getChildElements(head);
        assertEquals(2, values.size());

    }

    /**
     * Initialize a new trace based using the input file path
     *
     * @param traceFile
     *            The trace file
     * @return The trace
     */
    public static @NonNull ITmfTrace initializeTrace(String traceFile) {
        /* Initialize the trace */
        TmfXmlTraceStub trace = new TmfXmlTraceStub();
        try {
            trace.initTrace(null, Activator.getAbsolutePath(new Path(traceFile)).toOSString(), TmfEvent.class);
        } catch (TmfTraceException e1) {
            fail(e1.getMessage());
        }
        return trace;
    }

    /**
     * Initialize a new module using the xml file
     *
     * @param xmlAnalysisFile
     *            The xml file used to initialize the module
     * @return The module
     */
    public static @NonNull XmlStateSystemModule initializeModule(TmfXmlTestFiles xmlAnalysisFile) {

        /* Initialize the state provider module */
        Document doc = xmlAnalysisFile.getXmlDocument();
        assertNotNull(doc);

        /* get State Providers modules */
        NodeList stateproviderNodes = doc.getElementsByTagName(TmfXmlStrings.STATE_PROVIDER);
        assertFalse(stateproviderNodes.getLength() == 0);

        Element node = (Element) stateproviderNodes.item(0);
        XmlStateSystemModule module = new XmlStateSystemModule();
        String moduleId = node.getAttribute(TmfXmlStrings.ID);
        assertNotNull(moduleId);
        module.setId(moduleId);

        module.setXmlFile(xmlAnalysisFile.getPath());

        return module;
    }

    /**
     * This function test the data provided by the state intervals queried
     *
     * @param testId
     *            The id of the test
     * @param ss
     *            The state system associated to this test
     * @param quark
     *            The quark we want to query
     * @param expectedStarts
     *            The expected start timestamps for the intervals generated for
     *            this quark
     * @param expectedValues
     *            The expected content values for this quark
     * @throws AttributeNotFoundException
     *             If the quark we want to query is invalid
     * @throws StateSystemDisposedException
     *             If the state system has been disposed before the end of the
     *             queries
     */
    public static void verifyStateIntervals(String testId, @NonNull ITmfStateSystem ss, Integer quark, int[] expectedStarts, ITmfStateValue[] expectedValues) throws AttributeNotFoundException, StateSystemDisposedException {
        int expectedCount = expectedStarts.length - 1;
        List<ITmfStateInterval> intervals = StateSystemUtils.queryHistoryRange(ss, quark, expectedStarts[0], expectedStarts[expectedCount]);
        assertEquals(testId + ": Interval count", expectedCount, intervals.size());
        for (int i = 0; i < expectedCount; i++) {
            ITmfStateInterval interval = intervals.get(i);
            assertEquals(testId + ": Start time of interval " + i, expectedStarts[i], interval.getStartTime());
            long actualEnd = (i == expectedCount - 1) ? (expectedStarts[i + 1]) : (expectedStarts[i + 1]) - 1;
            assertEquals(testId + ": End time of interval " + i, actualEnd, interval.getEndTime());
            assertEquals(testId + ": Expected value of interval " + i, expectedValues[i], interval.getStateValue());
        }
    }

}
