/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.ui.swtbot.tests.latency;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.SystemCall;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.SystemCall.InitialInfo;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.AbstractSegmentStoreTableViewer;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.latency.SystemCallLatencyView;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of the latency table
 *
 * @author Matthew Khouzam
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class SystemCallLatencyTableAnalysisTest {

    private static final String TRACE_TYPE = "org.eclipse.linuxtools.lttng2.kernel.tracetype";
    private static final String PROJECT_NAME = "test";
    private static final String VIEW_ID = SystemCallLatencyView.ID;
    private static final String TRACING_PERSPECTIVE_ID = "org.eclipse.linuxtools.tmf.ui.perspective";

    /** The Log4j logger instance. */
    private static final Logger fLogger = Logger.getRootLogger();
    private SystemCallLatencyView fLatencyView;
    private AbstractSegmentStoreTableViewer fTable;

    /**
     * Things to setup
     */
    @BeforeClass
    public static void beforeClass() {

        SWTBotUtils.initialize();
        Thread.currentThread().setName("SWTBotTest");
        /* set up for swtbot */
        SWTBotPreferences.TIMEOUT = 20000; /* 20 second timeout */
        SWTBotPreferences.KEYBOARD_LAYOUT = "EN_US";
        fLogger.removeAllAppenders();
        fLogger.addAppender(new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT));
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        final List<SWTBotView> openViews = bot.views();
        for (SWTBotView view : openViews) {
            if (view.getTitle().equals("Welcome")) {
                view.close();
                bot.waitUntil(ConditionHelpers.ViewIsClosed(view));
            }
        }
        /* Switch perspectives */
        switchTracingPerspective();
        /* Finish waiting for eclipse to load */
        SWTBotUtils.waitForJobs();

    }

    /**
     * Opens a latency table
     */
    @Before
    public void createTable() {
        /*
         * Open latency view
         */
        SWTBotUtils.openView(VIEW_ID);
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotView viewBot = bot.viewById(VIEW_ID);
        final IViewReference viewReference = viewBot.getViewReference();
        IViewPart viewPart = UIThreadRunnable.syncExec(new Result<IViewPart>() {
            @Override
            public IViewPart run() {
                return viewReference.getView(true);
            }
        });
        assertNotNull(viewPart);
        if (!(viewPart instanceof SystemCallLatencyView)) {
            fail("Could not instanciate view");
        }
        fLatencyView = (SystemCallLatencyView) viewPart;
        fTable = fLatencyView.getSegmentStoreViewer();
        assertNotNull(fTable);
    }

    /**
     * Closes the view
     */
    @After
    public void closeTable() {
        final SWTWorkbenchBot swtWorkbenchBot = new SWTWorkbenchBot();
        SWTBotView viewBot = swtWorkbenchBot.viewById(VIEW_ID);
        viewBot.close();
    }

    private static void switchTracingPerspective() {
        final Exception retE[] = new Exception[1];
        if (!UIThreadRunnable.syncExec(new BoolResult() {
            @Override
            public Boolean run() {
                try {
                    PlatformUI.getWorkbench().showPerspective(TRACING_PERSPECTIVE_ID,
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                } catch (WorkbenchException e) {
                    retE[0] = e;
                    return false;
                }
                return true;
            }
        })) {
            fail(retE[0].getMessage());
        }

    }

    /**
     * Test incrementing
     */
    @Test
    public void climbTest() {
        List<@NonNull SystemCall> fixture = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            fixture.add(new SystemCall(new InitialInfo(i, "", Collections.EMPTY_MAP), 2 * i, 0));
        }

        assertNotNull(fTable);
        fTable.updateModel(fixture);
        SWTBotTable tableBot = new SWTBotTable(fTable.getTableViewer().getTable());
        SWTBot bot = new SWTBot();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "0", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "0", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "99", 0, 2));
    }

    /**
     * Test decrementing
     */
    @Test
    public void decrementingTest() {
        List<@NonNull SystemCall> fixture = new ArrayList<>();
        for (int i = 100; i >= 0; i--) {
            fixture.add(new SystemCall(new InitialInfo(i, "", Collections.EMPTY_MAP), 2 * i, 0));
        }
        assertNotNull(fTable);
        fTable.updateModel(fixture);
        SWTBotTable tableBot = new SWTBotTable(fTable.getTableViewer().getTable());
        SWTBot bot = new SWTBot();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "100", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "0", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "100", 0, 2));
    }

    /**
     * Test small table
     */
    @Test
    public void smallTest() {
        List<@NonNull SystemCall> fixture = new ArrayList<>();
        for (int i = 1; i >= 0; i--) {
            fixture.add(new SystemCall(new InitialInfo(i, "", Collections.EMPTY_MAP), 2 * i, 0));
        }
        assertNotNull(fTable);
        fTable.updateModel(fixture);
        SWTBotTable tableBot = new SWTBotTable(fTable.getTableViewer().getTable());
        SWTBot bot = new SWTBot();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "1", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "0", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "1", 0, 2));
    }

    /**
     * Test large
     */
    @Test
    public void largeTest() {
        final int size = 1000000;
        SystemCall[] fixture = new SystemCall[size];
        for (int i = 0; i < size; i++) {
            fixture[i] = (new SystemCall(new InitialInfo(i, "", Collections.EMPTY_MAP), 2 * i, 0));
        }
        assertNotNull(fTable);
        fTable.updateModel(fixture);
        SWTBotTable tableBot = new SWTBotTable(fTable.getTableViewer().getTable());
        SWTBot bot = new SWTBot();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "0", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "0", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "999999", 0, 2));
    }

    /**
     * Test noise
     */
    @Test
    public void noiseTest() {
        Random rnd = new Random();
        rnd.setSeed(1234);
        final int size = 1000000;
        SystemCall[] fixture = new SystemCall[size];
        for (int i = 0; i < size; i++) {
            int start = Math.abs(rnd.nextInt(100000000));
            int end = start + Math.abs(rnd.nextInt(1000000));
            fixture[i] = (new SystemCall(new InitialInfo(start, "", Collections.EMPTY_MAP), end, 0));
        }
        assertNotNull(fTable);
        fTable.updateModel(fixture);
        SWTBotTable tableBot = new SWTBotTable(fTable.getTableViewer().getTable());
        SWTBot bot = new SWTBot();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "894633", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "0", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "999999", 0, 2));
    }

    /**
     * Test gaussian noise
     */
    @Test
    public void gaussianNoiseTest() {
        Random rnd = new Random();
        rnd.setSeed(1234);
        List<@NonNull SystemCall> fixture = new ArrayList<>();
        for (int i = 1; i <= 1000000; i++) {
            int start = Math.abs(rnd.nextInt(100000000));
            final int delta = Math.abs(rnd.nextInt(1000));
            int end = start + delta * delta;
            fixture.add(new SystemCall(new InitialInfo(start, "", Collections.EMPTY_MAP), end, 0));
        }
        assertNotNull(fTable);
        fTable.updateModel(fixture);
        SWTBotTable tableBot = new SWTBotTable(fTable.getTableViewer().getTable());
        SWTBot bot = new SWTBot();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "400689", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "0", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "998001", 0, 2));
    }

    /**
     * Test with an actual trace, this is more of an integration test than a
     * unit test. This test is a slow one too. If some analyses are not well
     * configured, this test will also generates null pointer exceptions. These
     * are will be logged.
     *
     * @throws IOException
     *             trace not found?
     */
    @Test
    public void testWithTrace() throws IOException {
        String tracePath;
        tracePath = FileLocator.toFileURL(CtfTestTrace.ARM_64_BIT_HEADER.getTraceURL()).getPath();
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        SWTBotView view = bot.viewById(VIEW_ID);
        view.close();
        bot.waitUntil(ConditionHelpers.ViewIsClosed(view));
        SWTBotUtils.createProject(PROJECT_NAME);
        SWTBotUtils.openTrace(PROJECT_NAME, tracePath, TRACE_TYPE);
        SWTBotUtils.waitForJobs();
        createTable();
        SWTBotUtils.waitForJobs();
        SWTBotTable tableBot = new SWTBotTable(fTable.getTableViewer().getTable());
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "24100", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "1000", 0, 2));
        tableBot.header("Duration").click();
        bot.waitUntil(ConditionHelpers.isTableCellFilled(tableBot, "5904091700", 0, 2));
        bot.closeAllEditors();
        SWTBotUtils.deleteProject(PROJECT_NAME, bot);
    }
}
