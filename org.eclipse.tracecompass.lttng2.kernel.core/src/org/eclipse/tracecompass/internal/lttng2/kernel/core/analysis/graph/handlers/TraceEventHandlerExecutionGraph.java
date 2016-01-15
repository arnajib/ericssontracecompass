/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.handlers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex.EdgeDirection;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.LinuxValues;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.TcpEventStrings;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.building.LttngKernelExecGraphProvider;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.building.LttngKernelExecGraphProvider.Context;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.building.LttngKernelExecGraphProvider.ProcessStatus;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.model.EventField;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.model.LttngInterruptContext;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.model.LttngSystemModel;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.model.LttngWorker;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.event.matching.IMatchProcessingUnit;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventDependency;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Event handler that actually builds the execution graph from the events
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 */
public class TraceEventHandlerExecutionGraph extends BaseHandler {

    /*
     * The following IRQ constants was found empirically.
     *
     * TODO: other IRQ values should be determined from the lttng_statedump_interrupt events.
     */
    private static final int IRQ_TIMER = 0;

    private static final NullProgressMonitor DEFAULT_PROGRESS_MONITOR = new NullProgressMonitor();

    private final Table<String, Integer, LttngWorker> fKernel;
    private final IMatchProcessingUnit fMatchProcessing;
    private Map<ITmfEvent, TmfVertex> fTcpNodes;
    private TmfEventMatching fTcpMatching;

    /**
     * Constructor
     *
     * @param provider
     *            The parent graph provider
     */
    public TraceEventHandlerExecutionGraph(LttngKernelExecGraphProvider provider) {
        super(provider);
        fKernel = NonNullUtils.checkNotNull(HashBasedTable.create());

        fTcpNodes = new HashMap<>();
        fMatchProcessing = new IMatchProcessingUnit() {

            @Override
            public void matchingEnded() {
            }

            @Override
            public int countMatches() {
                return 0;
            }

            @Override
            public void addMatch(@Nullable TmfEventDependency match) {
                if (match == null) {
                    return;
                }
                TmfVertex output = fTcpNodes.remove(match.getSourceEvent());
                TmfVertex input = fTcpNodes.remove(match.getDestinationEvent());
                if (output != null && input != null) {
                    output.linkVertical(input).setType(EdgeType.NETWORK);
                }
            }

            @Override
            public void init(Collection<ITmfTrace> fTraces) {

            }

        };

        ITmfTrace trace = provider.getTrace();
        fTcpMatching = new TmfEventMatching(Collections.singleton(trace), fMatchProcessing);
        fTcpMatching.initMatching();
    }

    private LttngWorker getOrCreateKernelWorker(ITmfEvent event, Integer cpu) {
        String host = event.getTrace().getHostId();
        LttngWorker worker = fKernel.get(host, cpu);
        if (worker == null) {
            HostThread ht = new HostThread(host, -1);
            worker = new LttngWorker(ht, "kernel/" + cpu, event.getTimestamp().getValue()); //$NON-NLS-1$
            worker.setStatus(ProcessStatus.RUN);

            fKernel.put(host, cpu, worker);
        }
        return worker;
    }

    @Override
    public void handleEvent(ITmfEvent ev) {
        String eventName = ev.getName();
        IKernelAnalysisEventLayout eventLayout = getProvider().getEventLayout(ev.getTrace());

        if (eventName.equals(eventLayout.eventSchedSwitch())) {
            handleSchedSwitch(ev);
        } else if (eventName.equals(eventLayout.eventSoftIrqEntry())) {
            handleSoftirqEntry(ev);
        } else if (eventName.equals(TcpEventStrings.INET_SOCK_LOCAL_IN) ||
                eventName.equals(TcpEventStrings.NETIF_RECEIVE_SKB)) {
            handleInetSockLocalIn(ev);
        } else if (eventName.equals(TcpEventStrings.INET_SOCK_LOCAL_OUT) ||
                eventName.equals(TcpEventStrings.NET_DEV_QUEUE)) {
            handleInetSockLocalOut(ev);
        } else if (isWakeupEvent(ev)) {
            handleSchedWakeup(ev);
        }
    }

    private TmfVertex stateExtend(LttngWorker task, long ts) {
        TmfGraph graph = NonNullUtils.checkNotNull(getProvider().getAssignedGraph());
        TmfVertex node = new TmfVertex(ts);
        ProcessStatus status = task.getStatus();
        graph.append(task, node, resolveProcessStatus(status));
        return node;
    }

    private TmfVertex stateChange(LttngWorker task, long ts) {
        TmfGraph graph = NonNullUtils.checkNotNull(getProvider().getAssignedGraph());
        TmfVertex node = new TmfVertex(ts);
        ProcessStatus status = task.getOldStatus();
        graph.append(task, node, resolveProcessStatus(status));
        return node;
    }

    private static EdgeType resolveProcessStatus(ProcessStatus status) {
        EdgeType ret = EdgeType.UNKNOWN;
        switch (status) {
        case DEAD:
            break;
        case EXIT:
        case RUN:
            ret = EdgeType.RUNNING;
            break;
        case UNKNOWN:
            ret = EdgeType.UNKNOWN;
            break;
        case WAIT_BLOCKED:
            ret = EdgeType.BLOCKED;
            break;
        case WAIT_CPU:
        case WAIT_FORK:
            ret = EdgeType.PREEMPTED;
            break;
        case ZOMBIE:
            ret = EdgeType.UNKNOWN;
            break;
        default:
            break;
        }
        return ret;
    }

    private void handleSchedSwitch(ITmfEvent event) {
        String host = event.getTrace().getHostId();
        long ts = event.getTimestamp().getValue();
        IKernelAnalysisEventLayout eventLayout = getProvider().getEventLayout(event.getTrace());
        LttngSystemModel system = getProvider().getSystem();

        Integer next = EventField.getInt(event, eventLayout.fieldNextTid());
        Integer prev = EventField.getInt(event, eventLayout.fieldPrevTid());

        LttngWorker nextTask = system.findWorker(new HostThread(host, next));
        LttngWorker prevTask = system.findWorker(new HostThread(host, prev));

        if (prevTask == null || nextTask == null) {
            return;
        }
        stateChange(prevTask, ts);
        stateChange(nextTask, ts);
    }

    private void handleSchedWakeup(ITmfEvent event) {
        TmfGraph graph = NonNullUtils.checkNotNull(getProvider().getAssignedGraph());
        String host = event.getTrace().getHostId();
        Integer cpu = NonNullUtils.checkNotNull(TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event));
        IKernelAnalysisEventLayout eventLayout = getProvider().getEventLayout(event.getTrace());
        LttngSystemModel system = getProvider().getSystem();

        long ts = event.getTimestamp().getValue();
        Integer tid = EventField.getInt(event, eventLayout.fieldTid());

        LttngWorker target = system.findWorker(new HostThread(host, tid));
        LttngWorker current = system.getWorkerOnCpu(host, cpu);
        if (target == null) {
            return;
        }

        ProcessStatus status = target.getOldStatus();
        switch (status) {
        case WAIT_FORK:
            waitFork(graph, ts, target, current);
            break;
        case WAIT_BLOCKED:
            waitBlocked(event, graph, host, cpu, eventLayout, system, ts, target, current);
            break;
        case DEAD:
        case EXIT:
        case RUN:
        case UNKNOWN:
        case WAIT_CPU:
        case ZOMBIE:
            break;
        default:
            break;
        }
    }

    private void waitBlocked(ITmfEvent event, TmfGraph graph, String host, Integer cpu, IKernelAnalysisEventLayout eventLayout, LttngSystemModel system, long ts, LttngWorker target, @Nullable LttngWorker current) {
        LttngInterruptContext context = system.peekContextStack(host, cpu);
        switch (context.getContext()) {
        case HRTIMER:
            // shortcut of appendTaskNode: resolve blocking source in situ
            graph.append(target, new TmfVertex(ts), EdgeType.TIMER);
            break;
        case IRQ:
            irq(graph, eventLayout, ts, target, context);
            break;
        case SOFTIRQ:
            softIrq(event, graph, cpu, eventLayout, ts, target, context);
            break;
        case IPI:
            graph.append(target, new TmfVertex(ts), EdgeType.IPI);
            break;
        case NONE:
            none(ts, target, current);
            break;
        default:
            break;
        }
    }

    private void softIrq(ITmfEvent event, TmfGraph graph, Integer cpu, IKernelAnalysisEventLayout eventLayout, long ts, LttngWorker target, LttngInterruptContext context) {
        TmfVertex wup = new TmfVertex(ts);
        TmfEdge l2 = graph.append(target, wup);
        if (l2 != null) {
            int vec = EventField.getLong(context.getEvent(), eventLayout.fieldVec()).intValue();
            l2.setType(resolveSoftirq(vec));
        }
        // special case for network related softirq
        Long vec = EventField.getLong(context.getEvent(), eventLayout.fieldVec());
        if (vec == LinuxValues.SOFTIRQ_NET_RX || vec == LinuxValues.SOFTIRQ_NET_TX) {
            // create edge if wake up is caused by incoming packet
            LttngWorker k = getOrCreateKernelWorker(event, cpu);
            TmfVertex tail = graph.getTail(k);
            if (tail != null && tail.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE) != null) {
                TmfVertex kwup = stateExtend(k, event.getTimestamp().getValue());
                kwup.linkVertical(wup);
            }
        }
    }

    private void none(long ts, LttngWorker target, @Nullable LttngWorker current) {
        // task context wakeup
        if (current != null) {
            TmfVertex n0 = stateExtend(current, ts);
            TmfVertex n1 = stateChange(target, ts);
            n0.linkVertical(n1);
        } else {
            stateChange(target, ts);
        }
    }

    private static void irq(TmfGraph graph, IKernelAnalysisEventLayout eventLayout, long ts, LttngWorker target, LttngInterruptContext context) {
        TmfEdge link = graph.append(target, new TmfVertex(ts));
        if (link != null) {
            int vec = EventField.getLong(context.getEvent(), eventLayout.fieldIrq()).intValue();
            link.setType(resolveIRQ(vec));
        }
    }

    private void waitFork(TmfGraph graph, long ts, LttngWorker target, @Nullable LttngWorker current) {
        if (current != null) {
            TmfVertex n0 = stateExtend(current, ts);
            TmfVertex n1 = stateChange(target, ts);
            graph.link(n0, n1);
        } else {
            stateChange(target, ts);
        }
    }

    private static EdgeType resolveIRQ(int vec) {
        EdgeType ret = EdgeType.UNKNOWN;
        switch (vec) {
        case IRQ_TIMER:
            ret = EdgeType.INTERRUPTED;
            break;
        default:
            ret = EdgeType.UNKNOWN;
            break;
        }
        return ret;
    }

    private static EdgeType resolveSoftirq(int vec) {
        EdgeType ret = EdgeType.UNKNOWN;
        switch (vec) {
        case LinuxValues.SOFTIRQ_HRTIMER:
        case LinuxValues.SOFTIRQ_TIMER:
            ret = EdgeType.TIMER;
            break;
        case LinuxValues.SOFTIRQ_BLOCK:
        case LinuxValues.SOFTIRQ_BLOCK_IOPOLL:
            ret = EdgeType.BLOCK_DEVICE;
            break;
        case LinuxValues.SOFTIRQ_NET_RX:
        case LinuxValues.SOFTIRQ_NET_TX:
            ret = EdgeType.NETWORK;
            break;
        case LinuxValues.SOFTIRQ_SCHED:
            ret = EdgeType.INTERRUPTED;
            break;
        default:
            ret = EdgeType.UNKNOWN;
            break;
        }
        return ret;
    }

    private void handleInetSockLocalIn(ITmfEvent event) {
        Integer cpu = NonNullUtils.checkNotNull(TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event));
        String host = event.getTrace().getHostId();
        LttngSystemModel system = getProvider().getSystem();

        LttngInterruptContext intCtx = system.peekContextStack(host, cpu);
        Context context = intCtx.getContext();
        if (context == Context.SOFTIRQ) {
            LttngWorker k = getOrCreateKernelWorker(event, cpu);
            TmfVertex endpoint = stateExtend(k, event.getTimestamp().getValue());
            fTcpNodes.put(event, endpoint);
            // TODO add actual progress monitor
            fTcpMatching.matchEvent(event, event.getTrace(), DEFAULT_PROGRESS_MONITOR);
        }
    }

    private void handleInetSockLocalOut(ITmfEvent event) {
        Integer cpu = NonNullUtils.checkNotNull(TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event));
        String host = event.getTrace().getHostId();
        LttngSystemModel system = getProvider().getSystem();

        LttngInterruptContext intCtx = system.peekContextStack(host, cpu);
        Context context = intCtx.getContext();

        LttngWorker sender = null;
        if (context == Context.NONE) {
            sender = system.getWorkerOnCpu(event.getTrace().getHostId(), cpu);
        } else if (context == Context.SOFTIRQ) {
            sender = getOrCreateKernelWorker(event, cpu);
        }
        if (sender == null) {
            return;
        }
        TmfVertex endpoint = stateExtend(sender, event.getTimestamp().getValue());
        fTcpNodes.put(event, endpoint);
        // TODO, add actual progress monitor
        fTcpMatching.matchEvent(event, event.getTrace(), new NullProgressMonitor());
    }

    private void handleSoftirqEntry(ITmfEvent event) {
        IKernelAnalysisEventLayout eventLayout = getProvider().getEventLayout(event.getTrace());
        TmfGraph graph = NonNullUtils.checkNotNull(getProvider().getAssignedGraph());
        Long vec = EventField.getLong(event, eventLayout.fieldVec());
        if (vec == LinuxValues.SOFTIRQ_NET_RX || vec == LinuxValues.SOFTIRQ_NET_TX) {
            Integer cpu = NonNullUtils.checkNotNull(TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event));
            LttngWorker k = getOrCreateKernelWorker(event, cpu);
            graph.add(k, new TmfVertex(event.getTimestamp().getValue()));
        }
    }

}
