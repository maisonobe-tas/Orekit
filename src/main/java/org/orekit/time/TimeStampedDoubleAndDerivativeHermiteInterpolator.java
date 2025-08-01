/* Copyright 2002-2025 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.time;

import org.hipparchus.analysis.interpolation.HermiteInterpolator;

import java.util.List;

/**
 * Hermite interpolator of time stamped double value.
 *
 * @author Vincent Cucchietti
 * @author Luc Maisonobe
 * @see HermiteInterpolator
 * @see TimeInterpolator
 */
public class TimeStampedDoubleAndDerivativeHermiteInterpolator
    extends AbstractTimeInterpolator<TimeStampedDoubleAndDerivative> {

    /**
     * Constructor with :
     * <ul>
     *     <li>Default number of interpolation points of {@code DEFAULT_INTERPOLATION_POINTS}</li>
     *     <li>Default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s)</li>
     * </ul>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     */
    public TimeStampedDoubleAndDerivativeHermiteInterpolator() {
        this(DEFAULT_INTERPOLATION_POINTS);
    }

    /**
     * Constructor with default extrapolation threshold value ({@code DEFAULT_EXTRAPOLATION_THRESHOLD_SEC} s).
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     */
    public TimeStampedDoubleAndDerivativeHermiteInterpolator(final int interpolationPoints) {
        this(interpolationPoints, DEFAULT_EXTRAPOLATION_THRESHOLD_SEC);
    }

    /**
     * Constructor.
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only with small number of interpolation
     * points (about 10-20 points) in order to avoid <a href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's
     * phenomenon</a> and numerical problems (including NaN appearing).
     *
     * @param interpolationPoints number of interpolation points
     * @param extrapolationThreshold extrapolation threshold beyond which the propagation will fail
     */
    public TimeStampedDoubleAndDerivativeHermiteInterpolator(final int interpolationPoints, final double extrapolationThreshold) {
        super(interpolationPoints, extrapolationThreshold);
    }

    /** {@inheritDoc} */
    @Override
    protected TimeStampedDoubleAndDerivative interpolate(final InterpolationData interpolationData) {
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // Fill interpolator with sample
        final AbsoluteDate                         interpolationDate = interpolationData.getInterpolationDate();
        final List<TimeStampedDoubleAndDerivative> neighborList      = interpolationData.getNeighborList();
        for (TimeStampedDoubleAndDerivative value : neighborList) {
            final double deltaT = value.getDate().durationFrom(interpolationDate);
            interpolator.addSamplePoint(deltaT,
                                        new double[] { value.getValue() },
                                        new double[] { value.getDerivative() });
        }

        final double[][] y = interpolator.derivatives(0, 1);
        return new TimeStampedDoubleAndDerivative(interpolationDate, y[0][0], y[1][0]);
    }

}
