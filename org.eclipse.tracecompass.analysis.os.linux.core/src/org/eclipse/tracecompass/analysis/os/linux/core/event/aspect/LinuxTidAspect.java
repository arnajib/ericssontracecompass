/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.event.aspect;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

/**
 * This aspect finds the ID of the thread that is running when the event
 * occurred.
 *
 * @author Geneviève Bastien
 * @since 1.0
 */
public abstract class LinuxTidAspect implements ITmfEventAspect {

    @Override
    public final String getName() {
        return Messages.getMessage(Messages.AspectName_Tid);
    }

    @Override
    public final String getHelpText() {
        return Messages.getMessage(Messages.AspectHelpText_Tid);
    }

    @Override
    public abstract @Nullable Integer resolve(ITmfEvent event);

}
