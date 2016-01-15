/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.core.model.readonly;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.analysis.xml.core.model.TmfXmlStateAttribute;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.IXmlStateSystemContainer;
import org.w3c.dom.Element;

/**
 * Implements a state attribute in a read only mode. See
 * {@link TmfXmlStateAttribute} for the syntax of this attribute.
 *
 * In read-only mode, attributes that are requested but do not exist in the
 * state system will not be added.
 *
 * @author Geneviève Bastien
 */
public class TmfXmlReadOnlyStateAttribute extends TmfXmlStateAttribute {

    /**
     * Constructor
     *
     * @param modelFactory
     *            The factory used to create XML model elements
     * @param attribute
     *            The XML element corresponding to this attribute
     * @param container
     *            The state system container this state value belongs to
     */
    public TmfXmlReadOnlyStateAttribute(TmfXmlReadOnlyModelFactory modelFactory, Element attribute, IXmlStateSystemContainer container) {
        super(modelFactory, attribute, container);
    }

    @Override
    protected int getQuarkAbsoluteAndAdd(String... path) throws AttributeNotFoundException {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            throw new IllegalStateException("The state system hasn't been initialized yet"); //$NON-NLS-1$
        }
        return ss.getQuarkAbsolute(path);
    }

    @Override
    protected int getQuarkRelativeAndAdd(int startNodeQuark, String... path) throws AttributeNotFoundException {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            throw new IllegalStateException("The state system hasn't been initialized yet"); //$NON-NLS-1$
        }
        return ss.getQuarkRelative(startNodeQuark, path);
    }

}
