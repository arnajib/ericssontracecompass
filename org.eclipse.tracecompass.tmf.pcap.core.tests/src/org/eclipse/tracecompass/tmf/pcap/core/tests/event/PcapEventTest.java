/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Vincent Perot - Initial API and implementation
 ******************************************************************************/

package org.eclipse.tracecompass.tmf.pcap.core.tests.event;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.internal.pcap.core.trace.BadPcapFileException;
import org.eclipse.tracecompass.internal.pcap.core.trace.PcapFile;
import org.eclipse.tracecompass.internal.tmf.pcap.core.event.PcapEvent;
import org.eclipse.tracecompass.internal.tmf.pcap.core.protocol.TmfPcapProtocol;
import org.eclipse.tracecompass.internal.tmf.pcap.core.trace.PcapTrace;
import org.eclipse.tracecompass.pcap.core.tests.shared.PcapTestTrace;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * JUnit that test the PcapEvent class.
 *
 * @author Vincent Perot
 */
public class PcapEventTest {

    private static PcapEvent fEvent;
    private static List<TmfPcapProtocol> fProtocolList;

    /**
     * Initialize the Packet and the EventField.
     *
     * @throws BadPcapFileException
     *             Thrown when the pcap file is erroneous.
     * @throws IOException
     *             Thrown when an IO error occurs.
     * @throws TmfTraceException
     *             Thrown when the trace is not valid.
     */
    @BeforeClass
    public static void setUp() throws IOException, BadPcapFileException, TmfTraceException {

        PcapTestTrace trace = PcapTestTrace.MOSTLY_TCP;
        assumeTrue(trace.exists());
        try (PcapFile pcap = new PcapFile(trace.getPath());) {
            PcapTrace pcapTrace = new PcapTrace();
            pcapTrace.initTrace(null, trace.getPath().toString(), PcapEvent.class);
            fEvent = pcapTrace.parseEvent(new TmfContext(new TmfLongLocation(3), 3));
            pcapTrace.dispose();
        }

        // Initialize protocol list.
        List<TmfPcapProtocol> list = new ArrayList<>();
        list.add(TmfPcapProtocol.PCAP);
        list.add(TmfPcapProtocol.ETHERNET_II);
        list.add(TmfPcapProtocol.IPV4);
        list.add(TmfPcapProtocol.TCP);
        list.add(TmfPcapProtocol.UNKNOWN);
        fProtocolList = ImmutableList.copyOf(list);
    }

    /**
     * Method that tests getProtocols of PcapEvent.
     */
    @Test
    public void getProtocolsTest() {
        assertEquals(fProtocolList, fEvent.getProtocols());
    }

    /**
     * Method that tests getMostEncapsulatedProtocol of PcapEvent.
     */
    @Test
    public void getMostEncapsulatedProtocolTest() {
        assertEquals(TmfPcapProtocol.TCP, fEvent.getMostEncapsulatedProtocol());
    }

    /**
     * Method that tests getFields of PcapEvent.
     */
    @Test
    public void getFieldsTest() {
        Map<String, String> map = fEvent.getFields(TmfPcapProtocol.IPV4);
        if (map == null) {
            fail("getFieldsTest() failed because map is null!");
            return;
        }
        assertEquals("145.254.160.237", map.get("Source IP Address"));
    }

    /**
     * Method that tests getPayload of PcapEvent.
     */
    @Test
    public void getPayloadTest() {
        ByteBuffer bb = fEvent.getPayload(TmfPcapProtocol.TCP);
        if (bb == null) {
            fail("getPayloadTest() failed because bb is null!");
            return;
        }
        assertEquals((byte) 0x47, bb.get());
    }

    /**
     * Method that tests getSourceEndpoint of PcapEvent.
     */
    @Test
    public void getSourceEndpointTest() {
        assertEquals("00:00:01:00:00:00/145.254.160.237/3372", fEvent.getSourceEndpoint(TmfPcapProtocol.TCP));
    }

    /**
     * Method that tests getDestinationEndpointTest of PcapEvent.
     */
    @Test
    public void getDestinationEndpointTest() {
        assertEquals("fe:ff:20:00:01:00", fEvent.getDestinationEndpoint(TmfPcapProtocol.ETHERNET_II));
    }

    /**
     * Method that tests toString() of PcapEvent.
     */
    @Test
    public void toStringTest() {
        assertEquals("3372 > 80 [ACK, PSH] Seq=951057940 Ack=290218380 Len=20", fEvent.toString());
    }

    /**
     * Method that tests toString(protocol) of PcapEvent.
     */
    @Test
    public void toStringAtSpecificProtocolTest() {
        assertEquals("Src: 145.254.160.237 , Dst: 65.208.228.223", fEvent.toString(TmfPcapProtocol.IPV4));
    }

}
