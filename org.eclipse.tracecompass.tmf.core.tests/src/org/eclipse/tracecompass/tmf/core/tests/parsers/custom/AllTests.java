/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.tests.parsers.custom;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for custom parsers
 * @author Matthew Khouzam
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        CustomXmlTraceInvalidTest.class,
        CustomXmlTraceBadlyFormedTest.class,
        CustomXmlTraceValidTest.class,
        CustomXmlIndexTest.class,
        CustomTxtIndexTest.class
})
public class AllTests {
}
