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

package org.eclipse.tracecompass.tmf.analysis.xml.ui.module;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.TmfXmlUiStrings;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.views.xychart.XmlXYView;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.ui.views.timegraph.XmlTimeGraphView;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisOutput;
import org.eclipse.tracecompass.tmf.core.analysis.ITmfNewAnalysisModuleListener;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class searches all XML files to find outputs applicable to the newly
 * created analysis
 *
 * @author Geneviève Bastien
 */
public class TmfXmlAnalysisOutputSource implements ITmfNewAnalysisModuleListener {

    /** String separating data elements for the output properties */
    public static final @NonNull String DATA_SEPARATOR = ";;;"; //$NON-NLS-1$

    /**
     * Enum to match the name of a view's XML element to its view ID.
     */
    public static enum ViewType {
        /**
         * Time graph view element
         */
        TIME_GRAPH_VIEW(TmfXmlUiStrings.TIME_GRAPH_VIEW, XmlTimeGraphView.ID),
        /**
         * XY chart view element
         */
        XY_VIEW(TmfXmlUiStrings.XY_VIEW, XmlXYView.ID);

        private final @NonNull String fXmlElem;
        private final String fViewId;

        private ViewType(@NonNull String xmlElem, String viewId) {
            fXmlElem = xmlElem;
            fViewId = viewId;
        }

        /**
         * Get the XML element corresponding to this view type
         *
         * @return The XML element corresponding to this type
         */
        public @NonNull String getXmlElem() {
            return fXmlElem;
        }

        private String getViewId() {
            return fViewId;
        }
    }


    @Override
    public void moduleCreated(IAnalysisModule module) {
        IPath pathToFiles = XmlUtils.getXmlFilesPath();
        File fFolder = pathToFiles.toFile();
        if (!(fFolder.isDirectory() && fFolder.exists())) {
            return;
        }
        for (File xmlFile : fFolder.listFiles()) {
            if (!XmlUtils.xmlValidate(xmlFile).isOK()) {
                continue;
            }

            try {
                /* Load the XML File */
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(xmlFile);
                doc.getDocumentElement().normalize();

                /* get state provider views if the analysis has state systems */
                if (module instanceof TmfStateSystemAnalysisModule) {
                    for (ViewType viewType : ViewType.values()) {
                        NodeList ssViewNodes = doc.getElementsByTagName(viewType.getXmlElem());
                        for (int i = 0; i < ssViewNodes.getLength(); i++) {
                            Element node = (Element) ssViewNodes.item(i);

                            /* Check if analysis is the right one */
                            List<Element> headNodes = XmlUtils.getChildElements(node, TmfXmlStrings.HEAD);
                            if (headNodes.size() != 1) {
                                continue;
                            }

                            List<Element> analysisNodes = XmlUtils.getChildElements(headNodes.get(0), TmfXmlStrings.ANALYSIS);
                            for (Element analysis : analysisNodes) {
                                String analysisId = analysis.getAttribute(TmfXmlStrings.ID);
                                if (analysisId.equals(module.getId())) {
                                    String viewId = viewType.getViewId();
                                    IAnalysisOutput output = new TmfXmlViewOutput(viewId, viewType);
                                    output.setOutputProperty(TmfXmlUiStrings.XML_OUTPUT_DATA, node.getAttribute(TmfXmlStrings.ID) + DATA_SEPARATOR + xmlFile.getAbsolutePath(), false);
                                    module.registerOutput(output);
                                }
                            }
                        }
                    }
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                Activator.logError("Error opening XML file", e); //$NON-NLS-1$
            }
        }
    }

}
