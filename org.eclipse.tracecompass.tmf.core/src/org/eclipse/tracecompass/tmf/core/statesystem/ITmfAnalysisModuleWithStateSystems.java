/*******************************************************************************
 * Copyright (c) 2013, 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.statesystem;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;

/**
 * Interface for analysis modules providing state systems.
 *
 * @author Geneviève Bastien
 */
public interface ITmfAnalysisModuleWithStateSystems extends IAnalysisModule {

    /**
     * Return a specific state system provided by this analysis.
     *
     * @param id
     *            The ID of the state system
     * @return The state system corresponding to the given ID, null if there is
     *         no match.
     */
    @Nullable ITmfStateSystem getStateSystem(String id);

    /**
     * Return all the state systems provided by this analysis module, in
     * Iterable format.
     *
     * @return The state systems
     */
    Iterable<ITmfStateSystem> getStateSystems();

}
