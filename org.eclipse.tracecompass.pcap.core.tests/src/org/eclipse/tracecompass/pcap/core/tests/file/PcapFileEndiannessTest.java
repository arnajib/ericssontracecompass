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
 *******************************************************************************/

package org.eclipse.tracecompass.pcap.core.tests.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;

import org.eclipse.tracecompass.internal.pcap.core.packet.BadPacketException;
import org.eclipse.tracecompass.internal.pcap.core.trace.BadPcapFileException;
import org.eclipse.tracecompass.internal.pcap.core.trace.PcapFile;
import org.eclipse.tracecompass.pcap.core.tests.shared.PcapTestTrace;
import org.junit.Test;

/**
 * JUnit Class that tests whether the Pcap parser can read both big endian and
 * little endian files.
 *
 * @author Vincent Perot
 */
public class PcapFileEndiannessTest {

    /**
     * Test that verify that two files with different endianness contain the
     * same packets.
     *
     * @throws BadPcapFileException
     *             Thrown when the file is erroneous. Fails the test.
     * @throws IOException
     *             Thrown when an IO error occurs. Fails the test.
     * @throws BadPacketException
     *             Thrown when a packet is erroneous. Fails the test.
     */
    @Test
    public void EndiannessTest() throws IOException, BadPcapFileException, BadPacketException {
        PcapTestTrace trace = PcapTestTrace.SHORT_LITTLE_ENDIAN;
        assumeTrue(trace.exists());
        Path path1 = trace.getPath();

        trace = PcapTestTrace.SHORT_LITTLE_ENDIAN;
        assumeTrue(trace.exists());
        Path path2 = PcapTestTrace.SHORT_BIG_ENDIAN.getPath();

        try (PcapFile littleEndian = new PcapFile(path1);
                PcapFile bigEndian = new PcapFile(path2);) {
            assertEquals(ByteOrder.BIG_ENDIAN, bigEndian.getByteOrder());
            assertEquals(ByteOrder.LITTLE_ENDIAN, littleEndian.getByteOrder());
            while (littleEndian.hasNextPacket() && bigEndian.hasNextPacket()) {
                assertEquals(littleEndian.parseNextPacket(), bigEndian.parseNextPacket());
            }
        }
    }
}
