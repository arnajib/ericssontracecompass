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

package org.eclipse.tracecompass.internal.pcap.core.filter;

import org.eclipse.tracecompass.internal.pcap.core.packet.Packet;
import org.eclipse.tracecompass.internal.pcap.core.protocol.PcapProtocol;

/**
 * Class used to filter the packets by protocol. This is used, for instance, to
 * build the packet streams.
 *
 * @author Vincent Perot
 */
public class PacketFilterByProtocol implements IPacketFilter {

    private final PcapProtocol fProtocol;

    /**
     * Constructor of the PacketFilterByProtocol class.
     *
     * @param protocol
     *            The protocol that the incoming packets must contain.
     */
    public PacketFilterByProtocol(PcapProtocol protocol) {
        fProtocol = protocol;
    }

    @Override
    public boolean accepts(Packet packet) {
        return packet.hasProtocol(fProtocol);
    }

    /**
     * Getter method for the protocol of this filter.
     *
     * @return The protocol of this filter.
     */
    public PcapProtocol getProtocol() {
        return fProtocol;
    }

}
