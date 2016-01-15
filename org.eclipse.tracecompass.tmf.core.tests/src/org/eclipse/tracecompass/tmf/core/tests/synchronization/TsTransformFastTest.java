/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francis Giraldeau - Initial implementation and API
 *   Geneviève Bastien - Fixes and improvements
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.tests.synchronization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.internal.tmf.core.synchronization.TmfTimestampTransformLinear;
import org.eclipse.tracecompass.internal.tmf.core.synchronization.TmfTimestampTransformLinearFast;
import org.eclipse.tracecompass.tmf.core.synchronization.ITmfTimestampTransform;
import org.junit.Test;

/**
 * Tests for {@link TmfTimestampTransformLinearFast}
 *
 * @author Geneviève Bastien
 */
public class TsTransformFastTest {

    private static final long ts = 1361657893526374091L;

    /**
     * Test whether the fast linear transform always yields the same value for
     * the same timestamp
     */
    @Test
    public void testFLTRepeatability() {
        TmfTimestampTransformLinearFast fast = new TmfTimestampTransformLinearFast(Math.PI, 0);
        // Access fDeltaMax to compute the cache range boundaries
        long deltaMax = fast.getDeltaMax();
        // Initialize the transform
        long timestamp = ts - (ts % deltaMax);
        fast.transform(timestamp);
        long tsMiss = timestamp + deltaMax;
        long tsNoMiss = timestamp + deltaMax - 1;

        // Get the transformed value to a timestamp without cache miss
        long tsTNoMiss = fast.transform(tsNoMiss);
        assertEquals(1, fast.getCacheMisses());

        // Cause a cache miss
        fast.transform(tsMiss);
        assertEquals(2, fast.getCacheMisses());

        /*
         * Get the transformed value of the same previous timestamp after the
         * miss
         */
        long tsTAfterMiss = fast.transform(tsNoMiss);
        assertEquals(tsTNoMiss, tsTAfterMiss);
    }

    /**
     * Test that 2 equal fast transform always give the same results for the
     * same values
     */
    @Test
    public void testFLTEquivalence() {
        TmfTimestampTransformLinearFast fast = new TmfTimestampTransformLinearFast(Math.PI, 0);
        TmfTimestampTransformLinearFast fast2 = new TmfTimestampTransformLinearFast(Math.PI, 0);

        long deltaMax = fast.getDeltaMax();

        long start = (ts - (ts % deltaMax) - 10);
        checkTime(fast, fast2, 20, start, 1);
    }

    /**
     * Test the precision of the fast timestamp transform compared to the
     * original transform.
     */
    @Test
    public void testFastTransformPrecision() {
        TmfTimestampTransformLinear precise = new TmfTimestampTransformLinear(Math.PI, 0);
        TmfTimestampTransformLinearFast fast = new TmfTimestampTransformLinearFast(Math.PI, 0);
        int samples = 100;
        long start = (long) Math.pow(10, 18);
        long end = Long.MAX_VALUE;
        int step = (int) ((end - start) / (samples * Math.PI));
        checkTime(precise, fast, samples, start, step);
        assertEquals(samples, fast.getCacheMisses());

        // check that rescale is done only when required
        // assumes tsBitWidth == 30
        // test forward and backward timestamps
        samples = 1000;
        int[] directions = new int[] { 1, -1 };
        for (Integer direction : directions) {
            for (int i = 0; i <= 30; i++) {
                fast.resetScaleStats();
                step = (1 << i) * direction;
                checkTime(precise, fast, samples, start, step);
                assertTrue(String.format("samples: %d scale misses: %d",
                        samples, fast.getCacheMisses()), samples >= fast.getCacheMisses());
            }
        }

    }

    /**
     * Test that fast transform produces the same result for small and large slopes.
     */
    @Test
    public void testFastTransformSlope() {
        int[] dir = new int[] { 1, -1 };
        long start = (1 << 30);
        for (int ex = -9; ex <= 9; ex++) {
            for (int d = 0; d < dir.length; d++) {
                double slope = Math.pow(10.0, ex);
                TmfTimestampTransformLinear precise = new TmfTimestampTransformLinear(slope, 0);
                TmfTimestampTransformLinearFast fast = new TmfTimestampTransformLinearFast(slope, 0);
                checkTime(precise, fast, 1000, start, dir[d]);
            }
        }
    }

    /**
     * Check that the proper exception are raised for illegal slopes
     */
    @Test
    public void testFastTransformArguments() {
        double[] slopes = new double[] { -1.0, ((double)Integer.MAX_VALUE) + 1, 1e-10 };
        for (double slope: slopes) {
            Exception exception = null;
            try {
                new TmfTimestampTransformLinearFast(slope, 0.0);
            } catch (IllegalArgumentException e) {
                exception = e;
            }
            assertNotNull(exception);
        }
    }

    private static void checkTime(ITmfTimestampTransform precise, ITmfTimestampTransform fast,
            int samples, long start, long step) {
        long prev = 0;
        for (int i = 0; i < samples; i++) {
            long time = start + i * step;
            long exp = precise.transform(time);
            long act = fast.transform(time);
            long err = act - exp;
            // allow only two ns of error
            assertTrue("[" + err + "]", Math.abs(err) < 3);
            if (i > 0) {
                if (step > 0) {
                    assertTrue("monotonic error" + act + " " + prev, act >= prev);
                } else if (step < 0) {
                    assertTrue("monotonic ", act <= prev);
                }
            }
            prev = act;
        }
    }

}
