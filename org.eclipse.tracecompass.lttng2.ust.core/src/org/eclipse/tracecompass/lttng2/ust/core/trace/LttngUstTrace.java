/**********************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *   Alexandre Montplaisir - Add UST callstack state system
 *   Marc-Andre Laperle - Handle BufferOverflowException (Bug 420203)
 **********************************************************************/

package org.eclipse.tracecompass.lttng2.ust.core.trace;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.lttng2.ust.core.Activator;
import org.eclipse.tracecompass.internal.lttng2.ust.core.trace.layout.LttngUst20EventLayout;
import org.eclipse.tracecompass.internal.lttng2.ust.core.trace.layout.LttngUst27EventLayout;
import org.eclipse.tracecompass.internal.lttng2.ust.core.trace.layout.LttngUst28EventLayout;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.UstDebugInfoBinaryAspect;
import org.eclipse.tracecompass.lttng2.ust.core.analysis.debuginfo.UstDebugInfoSourceAspect;
import org.eclipse.tracecompass.lttng2.ust.core.trace.layout.ILttngUstEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfUtils;

import com.google.common.collect.ImmutableSet;

/**
 * Class to contain LTTng-UST traces
 *
 * @author Matthew Khouzam
 */
public class LttngUstTrace extends CtfTmfTrace {

    /**
     * Name of the tracer that generates this trace type, as found in the CTF
     * metadata.
     *
     * @since 2.0
     */
    public static final String TRACER_NAME = "lttng-ust"; //$NON-NLS-1$

    private static final int CONFIDENCE = 100;

    private static final @NonNull Collection<ITmfEventAspect> LTTNG_UST_ASPECTS;

    static {
        ImmutableSet.Builder<ITmfEventAspect> builder = ImmutableSet.builder();
        builder.addAll(CtfTmfTrace.CTF_ASPECTS);
        builder.add(UstDebugInfoBinaryAspect.INSTANCE);
        builder.add(UstDebugInfoSourceAspect.INSTANCE);
        LTTNG_UST_ASPECTS = NonNullUtils.checkNotNull(builder.build());
    }

    private @Nullable ILttngUstEventLayout fLayout = null;

    /**
     * Default constructor
     */
    public LttngUstTrace() {
        super(LttngUstEventFactory.instance());
    }

    /**
     * Get the event layout to use with this trace. This normally depends on the
     * tracer's version.
     *
     * @return The event layout
     * @since 2.0
     */
    public @NonNull ILttngUstEventLayout getEventLayout() {
        ILttngUstEventLayout layout = fLayout;
        if (layout == null) {
            throw new IllegalStateException("Cannot get the layout of a non-initialized trace!"); //$NON-NLS-1$
        }
        return layout;
    }

    @Override
    public void initTrace(IResource resource, String path,
            Class<? extends ITmfEvent> eventType) throws TmfTraceException {
        super.initTrace(resource, path, eventType);

        /* Determine the event layout to use from the tracer's version */
        fLayout = getLayoutFromEnv();
    }

    private @NonNull ILttngUstEventLayout getLayoutFromEnv() {
        String tracerName = CtfUtils.getTracerName(this);
        int tracerMajor = CtfUtils.getTracerMajorVersion(this);
        int tracerMinor = CtfUtils.getTracerMinorVersion(this);

        if (TRACER_NAME.equals(tracerName)) {
            if (tracerMajor >= 2) {
                if (tracerMinor >= 8) {
                    return LttngUst28EventLayout.getInstance();
                } else if (tracerMinor >= 7) {
                    return LttngUst27EventLayout.getInstance();
                }
                return LttngUst20EventLayout.getInstance();
            }
        }

        /* Fallback to the UST 2.0 layout and hope for the best */
        return LttngUst20EventLayout.getInstance();
    }

    @Override
    public Iterable<ITmfEventAspect> getEventAspects() {
        return LTTNG_UST_ASPECTS;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation sets the confidence to 100 if the trace is a valid
     * CTF trace in the "ust" domain.
     */
    @Override
    public IStatus validate(final IProject project, final String path) {
        IStatus status = super.validate(project, path);
        if (status instanceof CtfTraceValidationStatus) {
            Map<String, String> environment = ((CtfTraceValidationStatus) status).getEnvironment();
            /* Make sure the domain is "ust" in the trace's env vars */
            String domain = environment.get("domain"); //$NON-NLS-1$
            if (domain == null || !domain.equals("\"ust\"")) { //$NON-NLS-1$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, Messages.LttngUstTrace_DomainError);
            }
            return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
        }
        return status;
    }
}
