/*******************************************************************************
 * Copyright (c) 2010, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.filter.model;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Filter node for the 'or' operation
 *
 * @version 1.0
 * @author Patrick Tasse
 */
public class TmfFilterOrNode extends TmfFilterTreeNode {

    /** or node name */
    public static final String NODE_NAME = "OR"; //$NON-NLS-1$
    /** not attribute name */
    public static final String NOT_ATTR = "not"; //$NON-NLS-1$

    private boolean fNot = false;

    /**
     * @param parent the parent node
     */
    public TmfFilterOrNode(ITmfFilterTreeNode parent) {
        super(parent);
    }

    @Override
    public String getNodeName() {
        return NODE_NAME;
    }

    /**
     * @return the NOT state
     */
    public boolean isNot() {
        return fNot;
    }

    /**
     * @param not the NOT state
     */
    public void setNot(boolean not) {
        this.fNot = not;
    }

    @Override
    public boolean matches(ITmfEvent event) {
        for (ITmfFilterTreeNode node : getChildren()) {
            if (node.matches(event)) {
                return true ^ fNot;
            }
        }
        return false & fNot;
    }

    @Override
    public String toString(boolean explicit) {
        StringBuffer buf = new StringBuffer();
        if (fNot) {
            buf.append("not "); //$NON-NLS-1$
        }
        if (getParent() != null && !(getParent() instanceof TmfFilterRootNode) && !(getParent() instanceof TmfFilterNode)) {
            buf.append("( "); //$NON-NLS-1$
        }
        for (int i = 0; i < getChildrenCount(); i++) {
            ITmfFilterTreeNode node = getChildren()[i];
            buf.append(node.toString(explicit));
            if (i < getChildrenCount() - 1) {
                buf.append(" or "); //$NON-NLS-1$
            }
        }
        if (getParent() != null && !(getParent() instanceof TmfFilterRootNode) && !(getParent() instanceof TmfFilterNode)) {
            buf.append(" )"); //$NON-NLS-1$
        }
        return buf.toString();
    }
}
