/* Copyright 2002-2025 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.estimation.measurements.generation;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.FDOA;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

import java.util.Map;

/** Builder for {@link FDOA} measurements.
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class FDOABuilder extends AbstractMeasurementBuilder<FDOA> {

    /** Prime ground station. */
    private final GroundStation primeStation;

    /** Second ground station. */
    private final GroundStation secondStation;

    /** Centre frequency of the signal emitted from the satellite. */
    private final double centreFrequency;

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param primeStation ground station that gives the date of the measurement
     * @param secondStation ground station that gives the measurement
     * @param centreFrequency satellite emitter frequency
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public FDOABuilder(final CorrelatedRandomVectorGenerator noiseSource,
                       final GroundStation primeStation,
                       final GroundStation secondStation,
                       final double centreFrequency,
                       final double sigma, final double baseWeight,
                       final ObservableSatellite satellite) {
        super(noiseSource, sigma, baseWeight, satellite);
        this.primeStation    = primeStation;
        this.secondStation   = secondStation;
        this.centreFrequency = centreFrequency;
    }

    /** {@inheritDoc} */
    @Override
    protected FDOA buildObserved(final AbsoluteDate date,
                                 final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new FDOA(primeStation, secondStation, centreFrequency,
                        date, Double.NaN,
                        getTheoreticalStandardDeviation()[0],
                        getBaseWeight()[0], getSatellites()[0]);
    }

}
