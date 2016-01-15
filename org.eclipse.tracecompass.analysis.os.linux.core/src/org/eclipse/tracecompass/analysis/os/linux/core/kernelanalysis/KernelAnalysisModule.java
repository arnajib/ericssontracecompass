/*******************************************************************************
 * Copyright (c) 2013, 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *   Mathieu Rail - Provide the requirements of the analysis
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.KernelStateProvider;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State System Module for lttng kernel traces
 *
 * @author Geneviève Bastien
 * @since 1.0
 */
public class KernelAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * The file name of the History Tree
     */
    public static final String HISTORY_TREE_FILE_NAME = "stateHistory.ht"; //$NON-NLS-1$

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.kernel"; //$NON-NLS-1$

    /*
     * TODO: Decide which events should be mandatory for the analysis, once the
     * appropriate error messages and session setup are in place.
     */
//    private static final ImmutableSet<String> REQUIRED_EVENTS = ImmutableSet.of();
//
//    private static final ImmutableSet<String> OPTIONAL_EVENTS = ImmutableSet.of(
            // FIXME These cannot be declared statically anymore, they depend on
            // the OriginTracer of the kernel trace.
            // LttngStrings.EXIT_SYSCALL,
            // LttngStrings.IRQ_HANDLER_ENTRY,
            // LttngStrings.IRQ_HANDLER_EXIT,
            // LttngStrings.SOFTIRQ_ENTRY,
            // LttngStrings.SOFTIRQ_EXIT,
            // LttngStrings.SOFTIRQ_RAISE,
            // LttngStrings.SCHED_PROCESS_FORK,
            // LttngStrings.SCHED_PROCESS_EXIT,
            // LttngStrings.SCHED_PROCESS_FREE,
            // LttngStrings.SCHED_SWITCH,
            // LttngStrings.STATEDUMP_PROCESS_STATE,
            // LttngStrings.SCHED_WAKEUP,
            // LttngStrings.SCHED_WAKEUP_NEW,
            //
            // /* FIXME Add the prefix for syscalls */
            // LttngStrings.SYSCALL_PREFIX
            // );

    /** The requirements as an immutable set */
    private static final Set<TmfAnalysisRequirement> REQUIREMENTS;

    static {
//        /* initialize the requirement: domain and events */
//        TmfAnalysisRequirement domainReq = new TmfAnalysisRequirement(SessionConfigStrings.CONFIG_ELEMENT_DOMAIN);
//        domainReq.addValue(SessionConfigStrings.CONFIG_DOMAIN_TYPE_KERNEL, ValuePriorityLevel.MANDATORY);
//
//        TmfAnalysisRequirement eventReq = new TmfAnalysisRequirement(SessionConfigStrings.CONFIG_ELEMENT_EVENT, REQUIRED_EVENTS, ValuePriorityLevel.MANDATORY);
//        eventReq.addValues(OPTIONAL_EVENTS, ValuePriorityLevel.OPTIONAL);
//
//        REQUIREMENTS = checkNotNull(ImmutableSet.of(domainReq, eventReq));
        REQUIREMENTS = Collections.EMPTY_SET;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());
        IKernelAnalysisEventLayout layout;

        if (trace instanceof IKernelTrace) {
            layout = ((IKernelTrace) trace).getKernelEventLayout();
        } else {
            /* Fall-back to the base LttngEventLayout */
            layout = IKernelAnalysisEventLayout.DEFAULT_LAYOUT;
        }

        return new KernelStateProvider(trace, layout);
    }

    @Override
    @NonNull
    protected String getSsFileName() {
        return HISTORY_TREE_FILE_NAME;
    }

    @Override
    protected String getFullHelpText() {
        return NonNullUtils.nullToEmptyString(Messages.LttngKernelAnalysisModule_Help);
    }

    @Override
    public Iterable<TmfAnalysisRequirement> getAnalysisRequirements() {
        return REQUIREMENTS;
    }
}
