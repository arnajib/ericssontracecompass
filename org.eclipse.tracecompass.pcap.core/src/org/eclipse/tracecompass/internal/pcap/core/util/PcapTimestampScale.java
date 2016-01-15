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

package org.eclipse.tracecompass.internal.pcap.core.util;

/**
 * Enum for the different time precision for pcap files.
 *
 * @author Vincent Perot
 */
public enum PcapTimestampScale {

    /** Microsecond Pcap */
    MICROSECOND,
    /** Nanosecond Pcap */
    NANOSECOND
}
