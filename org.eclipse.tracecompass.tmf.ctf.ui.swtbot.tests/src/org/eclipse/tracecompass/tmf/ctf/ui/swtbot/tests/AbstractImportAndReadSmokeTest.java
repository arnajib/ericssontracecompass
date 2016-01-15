/*******************************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *                   (Extracted from ImportAndReadSmokeTest.java)
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ctf.ui.swtbot.tests;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.internal.tmf.ui.views.statistics.TmfStatisticsViewImpl;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ui.editors.TmfEventsEditor;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.views.histogram.HistogramView;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertySheet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * Abstract SWTBot Smoke test class.
 *
 * @author Matthew Khouzam
 * @author Bernd Hufmann
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class AbstractImportAndReadSmokeTest {

    /** Trace name */
    protected static final String TRACE_NAME = "scp_dest";
    /** Trace folder */
    protected static final String TRACE_FOLDER = "synctraces";
    /** Trace type name for generic CTF traces */
    protected static final String TRACE_TYPE_NAME = "Generic CTF Trace";
    /** A Generic CTF Trace */
    protected static final @NonNull CtfTestTrace fTrace = CtfTestTrace.SYNC_DEST;
    /** SWT BOT workbench reference */
    protected static SWTWorkbenchBot fBot;
    /** Wizard to use */
    protected static Wizard fWizard;

    /** The Log4j logger instance. */
    protected static final Logger fLogger = Logger.getRootLogger();

    /** Test Class setup */
    @BeforeClass
    public static void init() {
        SWTBotUtils.initialize();

        /* set up for swtbot */
        SWTBotPreferences.TIMEOUT = 50000; /* 50 second timeout */
        fLogger.removeAllAppenders();
        fLogger.addAppender(new NullAppender());
        fBot = new SWTWorkbenchBot();

        SWTBotUtils.closeView("welcome", fBot);

        SWTBotUtils.switchToTracingPerspective();
        /* finish waiting for eclipse to load */
        SWTBotUtils.waitForJobs();
    }

    /**
     * Test Class teardown
     */
    @AfterClass
    public static void terminate() {
        fLogger.removeAllAppenders();
    }

    /**
     * Creates a tracing projects
     */
    protected void createProject() {
        SWTBotUtils.focusMainWindow(fBot.shells());
        fBot.menu("File").menu("New").menu("Project...").click();

        fBot.shell("New Project").setFocus();
        SWTBotTree tree = fBot.tree();
        assertNotNull(tree);
        final String tracingKey = "Tracing";
        fBot.waitUntil(ConditionHelpers.IsTreeNodeAvailable(tracingKey, tree));
        final SWTBotTreeItem tracingNode = tree.expandNode(tracingKey);

        tracingNode.select();
        final String projectKey = "Tracing Project";
        fBot.waitUntil(ConditionHelpers.IsTreeChildNodeAvailable(projectKey, tracingNode));
        final SWTBotTreeItem tracingProject = tracingNode.getNode(projectKey);
        assertNotNull(tracingProject);

        tracingProject.select();
        tracingProject.click();

        SWTBotButton nextButton = fBot.button("Next >");
        fBot.waitUntil(Conditions.widgetIsEnabled(nextButton));
        nextButton.click();
        fBot.shell("Tracing Project").setFocus();

        final SWTBotText text = fBot.text();
        text.setText(getProjectName());

        fBot.button("Finish").click();
        SWTBotUtils.waitForJobs();
    }

    /**
     * Finishes the wizard
     */
    protected void importFinish() {
        SWTBotShell shell = fBot.activeShell();
        final SWTBotButton finishButton = fBot.button("Finish");
        finishButton.click();
        fBot.waitUntil(Conditions.shellCloses(shell));
        SWTBotUtils.waitForJobs();
    }

    /**
     * Gets the project Name
     *
     * @return the project name
     */
    protected abstract String getProjectName();

    // ---------------------------------------------
    // Helpers for testing views
    // ---------------------------------------------

    /**
     * Verifies the properties view for a given view part
     *
     * @param vp
     *            a view part
     */
    protected void testPropertyView(IViewPart vp) {
        PropertySheet pv = (PropertySheet) vp;
        assertNotNull(pv);
    }

    /**
     * Verifies the Histogram View
     *
     * @param vp
     *            the view part
     * @param tmfEd
     *            the events editor
     */
    protected void testHistogramView(IViewPart vp, final TmfEventsEditor tmfEd) {
        final CtfTmfEvent desiredEvent1 = getEvent(100);
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                tmfEd.setFocus();
                tmfEd.selectionChanged(new SelectionChangedEvent(tmfEd, new StructuredSelection(desiredEvent1)));
            }
        });

        SWTBotUtils.waitForJobs();
        SWTBotUtils.delay(1000);

        final CtfTmfEvent desiredEvent2 = getEvent(10000);
        SWTBotView hvBot = fBot.viewById(HistogramView.ID);
        List<SWTBotToolbarButton> hvTools = hvBot.getToolbarButtons();
        for (SWTBotToolbarButton hvTool : hvTools) {
            if (hvTool.getToolTipText().toLowerCase().contains("lost")) {
                hvTool.click();
            }
        }
        HistogramView hv = (HistogramView) vp;
        final TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(hv, desiredEvent1.getTimestamp());
        final TmfSelectionRangeUpdatedSignal signal2 = new TmfSelectionRangeUpdatedSignal(hv, desiredEvent2.getTimestamp());
        hv.updateTimeRange(100000);
        SWTBotUtils.waitForJobs();
        hv.selectionRangeUpdated(signal);
        hv.broadcast(signal);
        SWTBotUtils.waitForJobs();
        SWTBotUtils.delay(1000);

        hv.updateTimeRange(1000000000);
        SWTBotUtils.waitForJobs();
        hv.selectionRangeUpdated(signal2);
        hv.broadcast(signal2);
        SWTBotUtils.waitForJobs();
        SWTBotUtils.delay(1000);
        assertNotNull(hv);
    }

    /**
     * Verifies the statistics view
     *
     * @param vp
     *            the view part
     */
    protected void testStatisticsView(IViewPart vp) {
        TmfStatisticsViewImpl sv = (TmfStatisticsViewImpl) vp;
        assertNotNull(sv);
    }

    // ---------------------------------------------
    // Trace helpers
    // ---------------------------------------------

    /**
     * Gets an event at a given rank
     *
     * @param rank
     *            a rank
     * @return the event at given rank
     */
    protected CtfTmfEvent getEvent(int rank) {
        CtfTmfTrace trace = CtfTmfTestTraceUtils.getTrace(fTrace);
        ITmfContext ctx = trace.seekEvent(rank);
        CtfTmfEvent ret = trace.getNext(ctx);
        ctx.dispose();
        return ret;
    }

    /**
     * Gets a view part based on view title
     *
     * @param viewTile
     *            a view title
     * @return the view part
     */
    protected IViewPart getViewPart(final String viewTile) {
        final IViewPart[] vps = new IViewPart[1];
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                IViewReference[] viewRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
                for (IViewReference viewRef : viewRefs) {
                    IViewPart vp = viewRef.getView(true);
                    if (vp.getTitle().equals(viewTile)) {
                        vps[0] = vp;
                        return;
                    }
                }
            }
        });

        return vps[0];
    }
}
