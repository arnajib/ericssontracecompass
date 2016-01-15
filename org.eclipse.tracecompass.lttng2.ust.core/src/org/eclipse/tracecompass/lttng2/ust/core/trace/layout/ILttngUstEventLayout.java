/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Alexandre Montplaisir
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.ust.core.trace.layout;

/**
 * This interface defines concepts exposed by liblttng-ust libraries and traces.
 * Actual implementations can differ between different versions of the tracer.
 *
 * @author Alexandre Montplaisir
 * @since 2.0
 */
@SuppressWarnings("javadoc")
public interface ILttngUstEventLayout {

    // ------------------------------------------------------------------------
    // Event names
    // ------------------------------------------------------------------------

    /* liblttng-ust-libc-wrapper events */
    String eventLibcMalloc();
    String eventLibcCalloc();
    String eventLibcRealloc();
    String eventLibcFree();
    String eventLibcMemalign();
    String eventLibcPosixMemalign();

    /* liblttng-ust-dl events */
    String eventDlOpen();
    String eventDlClose();

    /* liblttng-ust-cyg-profile(-fast) events */
    String eventCygProfileFuncEntry();
    String eventCygProfileFastFuncEntry();
    String eventCygProfileFuncExit();
    String eventCygProfileFastFuncExit();

    // ------------------------------------------------------------------------
    // Field names
    // ------------------------------------------------------------------------

    String fieldPtr();
    String fieldNmemb();
    String fieldSize();
    String fieldOutPtr();
    String fieldInPtr();

    String fieldAddr();

    // ------------------------------------------------------------------------
    // Context field names
    // ------------------------------------------------------------------------

    String contextVpid();
    String contextVtid();
    String contextProcname();
    String contextIp();
}
