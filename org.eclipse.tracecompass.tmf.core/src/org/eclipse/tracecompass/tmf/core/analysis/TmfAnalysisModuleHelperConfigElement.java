/*******************************************************************************
 * Copyright (c) 2013, 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *   Mathieu Rail - Added functionality for getting a module's requirements
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.analysis;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.ContributorFactoryOSGi;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.analysis.TmfAnalysisModuleSourceConfigElement;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.osgi.framework.Bundle;

/**
 * Analysis module helper for modules provided by a plugin's configuration
 * elements.
 *
 * @author Geneviève Bastien
 */
public class TmfAnalysisModuleHelperConfigElement implements IAnalysisModuleHelper {

    private final IConfigurationElement fCe;

    /**
     * Constructor
     *
     * @param ce
     *            The source {@link IConfigurationElement} of this module helper
     */
    public TmfAnalysisModuleHelperConfigElement(IConfigurationElement ce) {
        fCe = ce;
    }

    // ----------------------------------------
    // Wrappers to {@link IAnalysisModule} methods
    // ----------------------------------------

    @Override
    public String getId() {
        String id = fCe.getAttribute(TmfAnalysisModuleSourceConfigElement.ID_ATTR);
        if (id == null) {
            throw new IllegalStateException();
        }
        return id;
    }

    @Override
    public String getName() {
        String name = fCe.getAttribute(TmfAnalysisModuleSourceConfigElement.NAME_ATTR);
        if (name == null) {
            throw new IllegalStateException();
        }
        return name;
    }

    @Override
    public boolean isAutomatic() {
        return Boolean.parseBoolean(fCe.getAttribute(TmfAnalysisModuleSourceConfigElement.AUTOMATIC_ATTR));
    }

    /**
     * @since 1.0
     */
    @Override
    public boolean appliesToExperiment() {
        return Boolean.parseBoolean(fCe.getAttribute(TmfAnalysisModuleSourceConfigElement.APPLIES_EXP_ATTR));
    }

    @Override
    public String getHelpText() {
        /*
         * FIXME: No need to externalize this. A better solution will be found
         * soon and this string is just temporary
         */
        return "The trace must be opened to get the help message"; //$NON-NLS-1$
    }

    @Override
    public String getIcon() {
        return fCe.getAttribute(TmfAnalysisModuleSourceConfigElement.ICON_ATTR);
    }

    @Override
    public Bundle getBundle() {
        return ContributorFactoryOSGi.resolve(fCe.getContributor());
    }

    @Override
    public boolean appliesToTraceType(Class<? extends ITmfTrace> traceclass) {
        boolean applies = false;

        /* Get the module's applying tracetypes */
        final IConfigurationElement[] tracetypeCE = fCe.getChildren(TmfAnalysisModuleSourceConfigElement.TRACETYPE_ELEM);
        for (IConfigurationElement element : tracetypeCE) {
            Class<?> applyclass;
            try {
                applyclass = getBundle().loadClass(element.getAttribute(TmfAnalysisModuleSourceConfigElement.CLASS_ATTR));
                String classAppliesVal = element.getAttribute(TmfAnalysisModuleSourceConfigElement.APPLIES_ATTR);
                boolean classApplies = true;
                if (classAppliesVal != null) {
                    classApplies = Boolean.parseBoolean(classAppliesVal);
                }
                if (classApplies) {
                    applies |= applyclass.isAssignableFrom(traceclass);
                } else {
                    /*
                     * If the trace type does not apply, reset the applies
                     * variable to false
                     */
                    if (applyclass.isAssignableFrom(traceclass)) {
                        applies = false;
                    }
                }
            } catch (ClassNotFoundException | InvalidRegistryObjectException e) {
                Activator.logError("Error in applies to trace", e); //$NON-NLS-1$
            }
        }
        return applies;
    }

    @Override
    public Iterable<Class<? extends ITmfTrace>> getValidTraceTypes() {
        Set<Class<? extends ITmfTrace>> traceTypes = new HashSet<>();

        for (TraceTypeHelper tth : TmfTraceType.getTraceTypeHelpers()) {
            if (appliesToTraceType(tth.getTraceClass())) {
                traceTypes.add(tth.getTraceClass());
            }
        }

        return traceTypes;
    }

    @Override
    public Iterable<TmfAnalysisRequirement> getAnalysisRequirements() {
        IAnalysisModule module = createModule();
        if (module != null) {
            return module.getAnalysisRequirements();
        }
        return Collections.EMPTY_SET;

    }

    // ---------------------------------------
    // Functionalities
    // ---------------------------------------

    private IAnalysisModule createModule() {
        IAnalysisModule module = null;
        try {
            module = (IAnalysisModule) fCe.createExecutableExtension(TmfAnalysisModuleSourceConfigElement.ANALYSIS_MODULE_ATTR);
            module.setName(getName());
            module.setId(getId());
        } catch (CoreException e) {
            Activator.logError("Error getting analysis modules from configuration files", e); //$NON-NLS-1$
        }
        return module;
    }

    @Override
    public IAnalysisModule newModule(ITmfTrace trace) throws TmfAnalysisException {

        /* Check if it applies to trace itself */
        boolean applies = appliesToTraceType(trace.getClass());
        /*
         * If the trace is an experiment, check if this module would apply to an
         * experiment should it apply to one of its traces.
         */
        if (!applies && (trace instanceof TmfExperiment) && appliesToExperiment()) {
            for (ITmfTrace expTrace : TmfTraceManager.getTraceSet(trace)) {
                if (appliesToTraceType(expTrace.getClass())) {
                    applies = true;
                    break;
                }
            }
        }

        if (!applies) {
            return null;
        }

        IAnalysisModule module = createModule();
        if (module == null) {
            return null;
        }

        module.setAutomatic(isAutomatic());

        /* Get the module's parameters */
        final IConfigurationElement[] parametersCE = fCe.getChildren(TmfAnalysisModuleSourceConfigElement.PARAMETER_ELEM);
        for (IConfigurationElement element : parametersCE) {
            String paramName = element.getAttribute(TmfAnalysisModuleSourceConfigElement.NAME_ATTR);
            if (paramName == null) {
                continue;
            }
            module.addParameter(paramName);
            String defaultValue = element.getAttribute(TmfAnalysisModuleSourceConfigElement.DEFAULT_VALUE_ATTR);
            if (defaultValue != null) {
                module.setParameter(paramName, defaultValue);
            }
        }
        if (module.setTrace(trace)) {
            TmfAnalysisManager.analysisModuleCreated(module);
        } else {
            module.dispose();
            module = null;
        }

        return module;

    }

    @Override
    public String getHelpText(@NonNull ITmfTrace trace) {
        IAnalysisModule module = createModule();
        if (module != null) {
            String ret = module.getHelpText(trace);
            module.dispose();
            return ret;
        }
        return getHelpText();

    }
}
