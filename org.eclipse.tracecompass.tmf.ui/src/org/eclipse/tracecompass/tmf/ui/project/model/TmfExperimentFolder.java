/*******************************************************************************
 * Copyright (c) 2010, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Patrick Tasse - Add support for folder elements
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.project.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tracecompass.tmf.ui.properties.ReadOnlyTextPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;

/**
 * Implementation of model element representing the unique "Experiments" folder
 * in the project.
 * <p>
 *
 * @version 1.0
 * @author Francois Chouinard
 *
 */
public class TmfExperimentFolder extends TmfProjectModelElement implements IPropertySource2 {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * The name of the experiment folder.
     */
    public static final String EXPER_FOLDER_NAME = "Experiments"; //$NON-NLS-1$

    // Property View stuff
    private static final String INFO_CATEGORY = "Info"; //$NON-NLS-1$
    private static final String NAME = "name"; //$NON-NLS-1$
    private static final String PATH = "path"; //$NON-NLS-1$
    private static final String LOCATION = "location"; //$NON-NLS-1$

    private static final ReadOnlyTextPropertyDescriptor NAME_DESCRIPTOR = new ReadOnlyTextPropertyDescriptor(NAME, NAME);
    private static final ReadOnlyTextPropertyDescriptor PATH_DESCRIPTOR = new ReadOnlyTextPropertyDescriptor(PATH, PATH);
    private static final ReadOnlyTextPropertyDescriptor LOCATION_DESCRIPTOR = new ReadOnlyTextPropertyDescriptor(LOCATION, LOCATION);

    private static final IPropertyDescriptor[] DESCRIPTORS = { NAME_DESCRIPTOR, PATH_DESCRIPTOR, LOCATION_DESCRIPTOR };

    static {
        NAME_DESCRIPTOR.setCategory(INFO_CATEGORY);
        PATH_DESCRIPTOR.setCategory(INFO_CATEGORY);
        LOCATION_DESCRIPTOR.setCategory(INFO_CATEGORY);
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor.
     * Creates a TmfExperimentFolder model element.
     * @param name The name of the folder
     * @param folder The folder reference
     * @param parent The parent (project element)
     */
    public TmfExperimentFolder(String name, IFolder folder, TmfProjectElement parent) {
        super(name, folder, parent);
        parent.addChild(this);
    }

    // ------------------------------------------------------------------------
    // TmfProjectModelElement
    // ------------------------------------------------------------------------

    @Override
    public IFolder getResource() {
        return (IFolder) fResource;
    }

    @Override
    void refreshChildren() {
        IFolder folder = getResource();

        // Get the children from the model
        Map<String, ITmfProjectModelElement> childrenMap = new HashMap<>();
        for (ITmfProjectModelElement element : getChildren()) {
            childrenMap.put(element.getResource().getName(), element);
        }

        try {
            IResource[] members = folder.members();
            for (IResource resource : members) {
                if (resource instanceof IFolder) {
                    IFolder expFolder = (IFolder) resource;
                    String name = resource.getName();
                    ITmfProjectModelElement element = childrenMap.get(name);
                    if (element instanceof TmfExperimentElement) {
                        childrenMap.remove(name);
                    } else {
                        element = new TmfExperimentElement(name, expFolder, this);
                    }
                    ((TmfExperimentElement) element).refreshChildren();
                }
            }
        } catch (CoreException e) {
        }

        // Cleanup dangling children from the model
        for (ITmfProjectModelElement danglingChild : childrenMap.values()) {
            removeChild(danglingChild);
        }
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Returns a list of experiment model elements under the experiments folder.
     * @return list of experiment model elements
     */
    public List<TmfExperimentElement> getExperiments() {
        List<ITmfProjectModelElement> children = getChildren();
        List<TmfExperimentElement> traces = new ArrayList<>();
        for (ITmfProjectModelElement child : children) {
            if (child instanceof TmfExperimentElement) {
                traces.add((TmfExperimentElement) child);
            }
        }
        return traces;
    }

    // ------------------------------------------------------------------------
    // IPropertySource2
    // ------------------------------------------------------------------------

    @Override
    public Object getEditableValue() {
        return null;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        return Arrays.copyOf(DESCRIPTORS, DESCRIPTORS.length);
    }

    @Override
    public Object getPropertyValue(Object id) {

        if (NAME.equals(id)) {
            return getName();
        }

        if (PATH.equals(id)) {
            return getPath().toString();
        }

        if (LOCATION.equals(id)) {
            return getLocation().toString();
        }

        return null;
    }

    @Override
    public void resetPropertyValue(Object id) {
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
    }

    @Override
    public boolean isPropertyResettable(Object id) {
        return false;
    }

    @Override
    public boolean isPropertySet(Object id) {
        return false;
    }

}
