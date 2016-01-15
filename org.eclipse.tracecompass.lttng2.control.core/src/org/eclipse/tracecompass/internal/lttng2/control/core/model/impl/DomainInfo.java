/**********************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.tracecompass.internal.lttng2.control.core.model.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.tracecompass.internal.lttng2.control.core.model.IChannelInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IDomainInfo;

/**
 * <p>
 * Implementation of the trace domain interface (IDomainInfo) to store domain
 * related data.
 * </p>
 *
 * @author Bernd Hufmann
 */
public class DomainInfo extends TraceInfo implements IDomainInfo {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /**
     * The channels information of the domain.
     */
    private final List<IChannelInfo> fChannels = new ArrayList<>();
    private boolean fIsKernel = false;
    private BufferType fBufferType = BufferType.BUFFER_TYPE_UNKNOWN;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Constructor
     * @param name - name of domain
     */
    public DomainInfo(String name) {
        super(name);
    }

    /**
     * Copy constructor
     * @param other - the instance to copy
     */
    public DomainInfo(DomainInfo other) {
        super(other);
        for (int i = 0; i < other.fChannels.size(); i++) {
            if (other.fChannels.get(i) instanceof ChannelInfo) {
                fChannels.add(new ChannelInfo((ChannelInfo)other.fChannels.get(i)));
            } else {
                fChannels.add(other.fChannels.get(i));
            }
        }
        fIsKernel = other.fIsKernel;
        fBufferType = other.fBufferType;
    }

    @Override
    public boolean isKernel() {
        return fIsKernel;
    }

    @Override
    public void setIsKernel(boolean isKernel) {
        fIsKernel = isKernel;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    @Override
    public IChannelInfo[] getChannels() {
        return fChannels.toArray(new IChannelInfo[fChannels.size()]);
    }

    @Override
    public void setChannels(List<IChannelInfo> channels) {
        fChannels.clear();
        for (Iterator<IChannelInfo> iterator = channels.iterator(); iterator.hasNext();) {
            IChannelInfo channelInfo = iterator.next();
            fChannels.add(channelInfo);
        }
    }

    @Override
    public void addChannel(IChannelInfo channel) {
        fChannels.add(channel);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + fChannels.hashCode();
        result = prime * result + (fIsKernel ? 1231 : 1237);
        result = prime * result + ((fBufferType == null) ? 0 : (fBufferType.ordinal() + 1));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DomainInfo other = (DomainInfo) obj;
        if (!fChannels.equals(other.fChannels)) {
            return false;
        }
        if (fIsKernel != other.fIsKernel) {
            return false;
        }
        if (fBufferType != other.fBufferType) {
            return false;
        }
        return true;
    }

    @Override
    public BufferType getBufferType() {
        if (fIsKernel) {
            return BufferType.BUFFER_SHARED;
        }
        return fBufferType;
    }

    @Override
    public void setBufferType(BufferType bufferType) {
        fBufferType = bufferType;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        StringBuffer output = new StringBuffer();
            output.append("[DomainInfo(");
            output.append(super.toString());
            output.append(",Channels=");
            if (fChannels.isEmpty()) {
                output.append("None");
            } else {
                for (Iterator<IChannelInfo> iterator = fChannels.iterator(); iterator.hasNext();) {
                    IChannelInfo channel = iterator.next();
                    output.append(channel.toString());
                }
            }
            output.append(",isKernel=");
            output.append(String.valueOf(fIsKernel));
            if ((fBufferType != null) && !fBufferType.equals(BufferType.BUFFER_TYPE_UNKNOWN) && !fBufferType.equals(BufferType.BUFFER_SHARED)) {
                output.append(",BufferType=");
                output.append(fBufferType);
            }
            output.append(")]");
            return output.toString();
    }
}
