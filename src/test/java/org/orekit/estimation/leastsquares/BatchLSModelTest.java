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
package org.orekit.estimation.leastsquares;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.ArrayRealVector;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.Incrementor;
import org.hipparchus.util.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.PVMeasurementCreator;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

import java.util.List;
import java.util.Map;

class BatchLSModelTest {

    @Test
    void testPerfectValue() {

        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final NumericalPropagatorBuilder[] builders = { propagatorBuilder };

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, 1.0, 300.0);
        final ParameterDriversList estimatedMeasurementsParameters = new ParameterDriversList();
        for (ObservedMeasurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if (driver.isSelected()) {
                    estimatedMeasurementsParameters.add(driver);
                }
            }
        }

        // create model
        final ModelObserver modelObserver = new ModelObserver() {
            /** {@inheritDoc} */
            @Override
            public void modelCalled(final Orbit[] newOrbits,
                                    final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> newEvaluations) {
                Assertions.assertEquals(1, newOrbits.length);
                Assertions.assertEquals(0, context.initialOrbit.getDate().durationFrom(newOrbits[0].getDate()),
                        1.0e-15);
                Assertions.assertEquals(0, Vector3D.distance(context.initialOrbit.getPosition(),
                                  newOrbits[0].getPosition()), 1.0e-15);
                Assertions.assertEquals(measurements.size(), newEvaluations.size());
            }
        };
        final BatchLSModel model = new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, modelObserver);
        model.setIterationsCounter(new Incrementor(100));
        model.setEvaluationsCounter(new Incrementor(100));

        // Test forward propagation flag to true
        Assertions.assertTrue(model.isForwardPropagation());

        // evaluate model on perfect start point
        final double[] normalizedProp = propagatorBuilder.getSelectedNormalizedParameters();
        final double[] normalized = new double[normalizedProp.length + estimatedMeasurementsParameters.getNbParams()];
        System.arraycopy(normalizedProp, 0, normalized, 0, normalizedProp.length);
        int i = normalizedProp.length;
        for (final ParameterDriver driver : estimatedMeasurementsParameters.getDrivers()) {
            normalized[i++] = driver.getNormalizedValue(new AbsoluteDate());
        }
        Pair<RealVector, RealMatrix> value = model.value(new ArrayRealVector(normalized));
        int index = 0;
        for (ObservedMeasurement<?> measurement : measurements) {
            for (int k = 0; k < measurement.getDimension(); ++k) {
                // the value is already a weighted residual
                Assertions.assertEquals(0.0, value.getFirst().getEntry(index++), 1.6e-7);
            }
        }
        Assertions.assertEquals(index, value.getFirst().getDimension());

    }

    @Test
    void testBackwardPropagation() {

        final Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final NumericalPropagatorBuilder[] builders = { propagatorBuilder };

        // create perfect PV measurements
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new PVMeasurementCreator(),
                                                               0.0, -1.0, 300.0);
        final ParameterDriversList estimatedMeasurementsParameters = new ParameterDriversList();
        for (ObservedMeasurement<?> measurement : measurements) {
            for (final ParameterDriver driver : measurement.getParametersDrivers()) {
                if (driver.isSelected()) {
                    estimatedMeasurementsParameters.add(driver);
                }
            }
        }

        // create model
        final ModelObserver modelObserver = new ModelObserver() {
            /** {@inheritDoc} */
            @Override
            public void modelCalled(final Orbit[] newOrbits,
                                    final Map<ObservedMeasurement<?>, EstimatedMeasurement<?>> newEvaluations) {
                // Do nothing here
            }
        };
        final BatchLSModel model = new BatchLSModel(builders, measurements, estimatedMeasurementsParameters, modelObserver);
        // Test forward propagation flag to false
        Assertions.assertFalse(model.isForwardPropagation());
    }

}



