/*******************************************************************************
 * Copyright (c) 2010, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tassé - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.parsers.custom;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;

/**
 * Event type for custom text parsers.
 *
 * @author Patrick Tassé
 */
public abstract class CustomEventType extends TmfEventType {

    /**
     * Constructor
     *
     * @param definition
     *            Trace definition
     */
    public CustomEventType(CustomTraceDefinition definition) {
        super(checkNotNull(definition.definitionName), getRootField(definition));
    }

    private static ITmfEventField getRootField(CustomTraceDefinition definition) {
        ITmfEventField[] fields = new ITmfEventField[definition.outputs.size()];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = new TmfEventField(definition.outputs.get(i).name, null, null);
        }
        ITmfEventField rootField = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);
        return rootField;
    }

}
