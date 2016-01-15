/*******************************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Vincent Perot - Initial API and implementation
 *   Alexandre Montplaisir - Update to new ITmfEventAspect API
 *   Patrick Tasse - Make pcap aspects singletons
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.pcap.core.trace;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.pcap.core.packet.BadPacketException;
import org.eclipse.tracecompass.internal.pcap.core.protocol.pcap.PcapPacket;
import org.eclipse.tracecompass.internal.pcap.core.trace.BadPcapFileException;
import org.eclipse.tracecompass.internal.pcap.core.trace.PcapFile;
import org.eclipse.tracecompass.internal.pcap.core.util.LinkTypeHelper;
import org.eclipse.tracecompass.internal.tmf.pcap.core.Activator;
import org.eclipse.tracecompass.internal.tmf.pcap.core.event.PcapEvent;
import org.eclipse.tracecompass.internal.tmf.pcap.core.event.aspect.PcapDestinationAspect;
import org.eclipse.tracecompass.internal.tmf.pcap.core.event.aspect.PcapProtocolAspect;
import org.eclipse.tracecompass.internal.tmf.pcap.core.event.aspect.PcapReferenceAspect;
import org.eclipse.tracecompass.internal.tmf.pcap.core.event.aspect.PcapSourceAspect;
import org.eclipse.tracecompass.internal.tmf.pcap.core.util.PcapEventFactory;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceProperties;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Class that represents a TMF Pcap Trace. It is used to make the glue between
 * the Pcap parser and TMF.
 *
 * TODO handle fields in TmfEventType for the filter view.
 *
 * @author Vincent Perot
 */
public class PcapTrace extends TmfTrace implements ITmfTraceProperties {

    /** pcap trace type id as defined in plugin.xml */
    public static final String TRACE_TYPE_ID = "org.eclipse.linuxtools.tmf.pcap.core.pcaptrace"; //$NON-NLS-1$

    private static final Collection<ITmfEventAspect> PCAP_ASPECTS =
            checkNotNull(ImmutableList.of(
                    ITmfEventAspect.BaseAspects.TIMESTAMP,
                    PcapSourceAspect.INSTANCE,
                    PcapDestinationAspect.INSTANCE,
                    PcapReferenceAspect.INSTANCE,
                    PcapProtocolAspect.INSTANCE,
                    ITmfEventAspect.BaseAspects.CONTENTS
                    ));

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    private static final int CONFIDENCE = 50;
    private @Nullable PcapFile fPcapFile;
    private @Nullable Map<String, String> fTraceProperties = null;

    @Override
    public synchronized ITmfLocation getCurrentLocation() {
        PcapFile pcap = fPcapFile;
        if (pcap == null) {
            return new TmfLongLocation(0);
        }
        return new TmfLongLocation(pcap.getCurrentRank());
    }

    @Override
    public synchronized double getLocationRatio(@Nullable ITmfLocation location) {
        TmfLongLocation loc = (TmfLongLocation) location;
        PcapFile pcap = fPcapFile;
        if (loc == null || pcap == null) {
            return 0;
        }
        try {
            return (pcap.getTotalNbPackets() == 0 ? 0 : ((double) loc.getLocationInfo()) / pcap.getTotalNbPackets());
        } catch (IOException | BadPcapFileException e) {
            String message = e.getMessage();
            if (message == null) {
                message = EMPTY_STRING;
            }
            Activator.logError(message, e);
            return 0;
        }

    }

    @Override
    public synchronized void initTrace(@Nullable IResource resource, @Nullable String path, @Nullable Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        if (path == null) {
            throw new TmfTraceException("No path has been specified."); //$NON-NLS-1$
        }
        Path filePath = checkNotNull(Paths.get(path));
        try {
            fPcapFile = new PcapFile(filePath);
        } catch (IOException | BadPcapFileException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public Iterable<ITmfEventAspect> getEventAspects() {
        return PCAP_ASPECTS;
    }

    @Override
    public synchronized @Nullable PcapEvent parseEvent(@Nullable ITmfContext context) {
        if (context == null) {
            return null;
        }

        long rank = context.getRank();
        PcapPacket packet = null;
        PcapFile pcap = fPcapFile;
        if (pcap == null) {
            return null;
        }
        try {
            pcap.seekPacket(rank);
            packet = pcap.parseNextPacket();
        } catch (ClosedChannelException e) {
            /*
             * This is handled independently and happens when the user closes
             * the trace while it is being parsed. It simply stops the parsing.
             * No need to log a error.
             */
            return null;
        } catch (IOException | BadPcapFileException | BadPacketException e) {
            String message = e.getMessage();
            if (message == null) {
                message = EMPTY_STRING;
            }
            Activator.logError(message, e);
            return null;
        }

        if (packet == null) {
            return null;
        }

        // Generate an event from this packet and return it.
        return PcapEventFactory.createEvent(packet, pcap, this);

    }

    @Override
    public synchronized ITmfContext seekEvent(double ratio) {
        long position;
        PcapFile pcap = fPcapFile;
        if (pcap == null) {
            return new TmfContext(new TmfLongLocation(0), 0);
        }

        try {
            /*
             * The ratio is between 0 and 1. We multiply it by the total number
             * of packets to get the position.
             */
            position = (long) (ratio * pcap.getTotalNbPackets());
        } catch (IOException | BadPcapFileException e) {
            String message = e.getMessage();
            if (message == null) {
                message = EMPTY_STRING;
            }
            Activator.logError(message, e);
            return new TmfContext(new TmfLongLocation(0), 0);
        }
        TmfLongLocation loc = new TmfLongLocation(position);
        return new TmfContext(loc, loc.getLocationInfo());
    }

    @Override
    public synchronized ITmfContext seekEvent(@Nullable ITmfLocation location) {
        TmfLongLocation loc = (TmfLongLocation) location;
        if (loc == null) {
            return new TmfContext(new TmfLongLocation(0));
        }

        return new TmfContext(loc, loc.getLocationInfo());
    }

    @Override
    public IStatus validate(@Nullable IProject project, @Nullable String path) {

        // All validations are made when making a new pcap file.
        if (path == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, EMPTY_STRING);
        }
        Path filePath = checkNotNull(Paths.get(path));
        try (PcapFile file = new PcapFile(filePath)) {
        } catch (IOException | BadPcapFileException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.toString());
        }
        return new TraceValidationStatus(CONFIDENCE, Activator.PLUGIN_ID);
    }

    @Override
    public synchronized void dispose() {
        super.dispose();
        PcapFile pcap = fPcapFile;
        if (pcap == null) {
            return;
        }
        try {
            pcap.close();
            fPcapFile = null;
        } catch (IOException e) {
            String message = e.getMessage();
            if (message == null) {
                message = EMPTY_STRING;
            }
            Activator.logError(message, e);
            return;
        }
    }

    @Override
    public synchronized Map<String, String> getTraceProperties() {
        PcapFile pcap = fPcapFile;
        if (pcap == null) {
            return Collections.emptyMap();
        }

        if (fTraceProperties != null) {
            return fTraceProperties;
        }

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put(nullToEmptyString(Messages.PcapTrace_Version), String.format("%d%c%d", pcap.getMajorVersion(), '.', pcap.getMinorVersion())); //$NON-NLS-1$
        builder.put(nullToEmptyString(Messages.PcapTrace_TimeZoneCorrection), pcap.getTimeZoneCorrection() + " s"); //$NON-NLS-1$
        builder.put(nullToEmptyString(Messages.PcapTrace_TimestampAccuracy), String.valueOf(pcap.getTimeAccuracy()));
        builder.put(nullToEmptyString(Messages.PcapTrace_MaxSnapLength), pcap.getSnapLength() + " bytes"); //$NON-NLS-1$
        builder.put(nullToEmptyString(Messages.PcapTrace_LinkLayerHeaderType), LinkTypeHelper.toString((int) pcap.getDataLinkType()) + " (" + pcap.getDataLinkType() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        builder.put(nullToEmptyString(Messages.PcapTrace_FileEndianness), nullToEmptyString(pcap.getByteOrder().toString()));

        fTraceProperties = builder.build();

        return fTraceProperties;
    }
}
