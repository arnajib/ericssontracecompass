/*******************************************************************************
 * Copyright (c) 2011, 2015, 2016 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mahdi Zolnouri - initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.os.linux.ui;

/**
 * @author Mahdi Zolnouri
 *
 * Names for generic icons and buttons used in Controlflow
 *
 */
@SuppressWarnings({"nls"})
public interface IControlflowImageConstants {

    /**
     * The path of the directory where the icons are placed.
     */
    String ICONS_PATH = "icons/"; //$NON-NLS-1$

    /* elcl16 */
    /**
     *  The optimize icon for scheduling algorithm
     */
    String IMG_UI_OPTIMIZE = ICONS_PATH + "elcl16/Optimization.png";

    /**
     * The flat view icon of the thread presentation.
     */
    String IMG_UI_FLAT_VIEW = ICONS_PATH + "elcl16/flat_view.png";

    /**
     * The hierarchical view icon of the thread presentation.
     */
    String IMG_UI_HIERARCHICAL_VIEW = ICONS_PATH + "elcl16/hierarchical_view.png";

}