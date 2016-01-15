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

package org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tracecompass.internal.lttng2.control.core.model.IBaseEventInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.IUstProviderInfo;
import org.eclipse.tracecompass.internal.lttng2.control.core.model.impl.UstProviderInfo;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.messages.Messages;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.model.ITraceControlComponent;
import org.eclipse.tracecompass.internal.lttng2.control.ui.views.property.UstProviderPropertySource;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * <p>
 * Implementation of the UST provider component.
 * </p>
 *
 * @author Bernd Hufmann
 */
public class UstProviderComponent extends TraceControlComponent {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------
    /**
     * Path to icon file for this component.
     */
    public static final String USTL_PROVIDER_ICON_FILE = "icons/obj16/targets.gif"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /**
     * The UST provider information.
     */
    private IUstProviderInfo fProviderInfo = null;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Constructor
     * @param name - the name of the component.
     * @param parent - the parent of this component.
     */
    public UstProviderComponent(String name, ITraceControlComponent parent) {
        super(name, parent);
        setImage(USTL_PROVIDER_ICON_FILE);
        setToolTip(Messages.TraceControl_ProviderDisplayName);
        fProviderInfo = new UstProviderInfo(name);
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------
    /**
     * Sets the UST provider information to the given value.
     * @param providerInfo - the provider information to set
     */
    public void setUstProvider(IUstProviderInfo providerInfo) {
        fProviderInfo = providerInfo;
        IBaseEventInfo[] events = providerInfo.getEvents();
        List<ITraceControlComponent> eventComponents = new ArrayList<>();
        for (int i = 0; i < events.length; i++) {
            BaseEventComponent component  = new BaseEventComponent(events[i].getName(), this);
            component.setEventInfo(events[i]);
            eventComponents.add(component);
        }
        setChildren(eventComponents);
        setName(getName() + " [PID=" + fProviderInfo.getPid() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * @return the process ID of the UST provider.
     */
    public int getPid() {
        return fProviderInfo.getPid();
    }

    /**
     * Sets the process ID of the UST provider to the given value.
     * @param pid - process ID to set
     */
    public void setPid(int pid) {
        fProviderInfo.setPid(pid);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IPropertySource.class) {
            return adapter.cast(new UstProviderPropertySource(this));
        }
        return null;
    }

}
