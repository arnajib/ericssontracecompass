/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bernd Hufmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.os.linux.core.latency.statistics;

import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * Class to calculate simple segment store statistics (min, max, average)
 *
 * @author Bernd Hufmann
 */
public class SegmentStoreStatistics {
    private long fMin;
    private long fMax;
    private long fNbSegments;
    private double fAverage;
    private double fVariance;

    /**
     * Constructor
     */
    public SegmentStoreStatistics() {
        fMin = Long.MAX_VALUE;
        fMax = Long.MIN_VALUE;
        fNbSegments = 0;
        fAverage = 0.0;
        fVariance = 0.0;
    }

    /**
     * Get minimum value
     *
     * @return minimum value
     */
    public long getMin() {
        return fMin;
    }

    /**
     * Get maximum value
     *
     * @return maximum value
     */
    public long getMax() {
        return fMax;
    }

    /**
     * Get number of segments analyzed
     *
     * @return number of segments analyzed
     */
    public long getNbSegments() {
        return fNbSegments;
    }

    /**
     * Gets the arithmetic average
     *
     * @return arithmetic average
     */
    public double getAverage() {
        return fAverage;
    }

    /**
     * Gets the standard deviation of the segments, uses the online algorithm
     * shown here <a href=
     * "https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm">
     * Wikipedia article of dec 3 2015 </a>
     *
     * @return the standard deviation of the segment store, will return NaN if
     *         there are less than 3 elements
     */
    public double getStdDev() {
        return fNbSegments > 2 ? Math.sqrt(fVariance / (fNbSegments - 1)) : Double.NaN;
    }

    /**
     * Update the statistics based on a given segment
     * <p>
     * This is an online algorithm and must retain a complexity of O(1)
     *
     * @param segment
     *            the segment used for the update
     */
    public void update(ISegment segment) {
        long value = segment.getLength();
        /*
         * Min and max are trivial, as well as number of segments
         */
        fMin = Math.min(fMin, value);
        fMax = Math.max(fMax, value);

        fNbSegments++;
        /*
         * The running mean is not trivial, see proof in javadoc.
         */
        double delta = value - fAverage;
        fAverage += delta / fNbSegments;
        fVariance += delta * (value - fAverage);
    }
}
