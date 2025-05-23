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
package org.orekit.estimation.measurements.generation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;

import java.util.Map;

/** Builder for {@link PV} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class PVBuilder extends AbstractMeasurementBuilder<PV> {

    /** Simple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param sigmaPosition theoretical standard deviation on position components
     * @param sigmaVelocity theoretical standard deviation on velocity components
     * @param baseWeight base weight
     * @param satellite satellite related to this builder
     */
    public PVBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                     final double sigmaPosition, final double sigmaVelocity,
                     final double baseWeight, final ObservableSatellite satellite) {
        super(noiseSource,
              new double[] {
                  sigmaPosition, sigmaPosition, sigmaPosition,
                  sigmaVelocity, sigmaVelocity, sigmaVelocity
              }, new double[] {
                  baseWeight, baseWeight, baseWeight,
                  baseWeight, baseWeight, baseWeight
              }, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected PV buildObserved(final AbsoluteDate date,
                               final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {
        return new PV(date, Vector3D.NaN, Vector3D.NaN,
                      getTheoreticalStandardDeviation()[0],
                      getTheoreticalStandardDeviation()[3],
                      getBaseWeight()[0], getSatellites()[0]);
    }

}
