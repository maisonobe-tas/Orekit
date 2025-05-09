/* Copyright 2002-2025 Mark Rutten
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
package org.orekit.estimation.measurements;

import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;

import java.util.List;

public class FDOATest {

    // Satellite transmission frequency
    private static final double CENTRE_FREQUENCY = 2.3e9;

    /**
     * Compare observed values and estimated values.
     * Both are calculated with a different algorithm.
     */
    @Test
    public void testValues() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create perfect measurements
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new FDOAMeasurementCreator(context, CENTRE_FREQUENCY),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        // Prepare statistics for values difference
        final StreamingStatistics diffStat = new StreamingStatistics();

        for (final ObservedMeasurement<?> measurement : measurements) {

            // Propagate to measurement date
            SpacecraftState state = propagator.propagate(measurement.getDate());

            // Estimate the measurement value
            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });

            // Store the difference between estimated and observed values in the stats
            diffStat.addValue(FastMath.abs(estimated.getEstimatedValue()[0] - measurement.getObservedValue()[0]));
        }

        // Mean and std errors check
        Assertions.assertEquals(0.0, diffStat.getMean(), 1e-3);
        Assertions.assertEquals(0.0, diffStat.getStandardDeviation(), 1e-3);

        // Test measurement type
        Assertions.assertEquals(FDOA.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
    }

    /**
     * Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference.
     */
    @Test
    public void testStateDerivatives() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // create perfect measurements
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new FDOAMeasurementCreator(context, CENTRE_FREQUENCY),
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            final AbsoluteDate    date  = measurement.getDate().shiftedBy(1);
            final SpacecraftState state = propagator.propagate(date);

            final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, new SpacecraftState[] { state });
            Assertions.assertEquals(3, estimated.getParticipants().length);
            final double[][] jacobian = estimated.getStateDerivatives(0);

            final double[][] finiteDifferencesJacobian =
                    Differentiation.differentiate(state1 -> measurement.
                        estimate(0, 0, new SpacecraftState[] { state1 }).
                        getEstimatedValue(), 1, propagator.getAttitudeProvider(),
                                                  OrbitType.CARTESIAN, PositionAngleType.TRUE, 15.0, 3).
                        value(state);

            Assertions.assertEquals(finiteDifferencesJacobian.length, jacobian.length);
            Assertions.assertEquals(finiteDifferencesJacobian[0].length, jacobian[0].length);

            for (int i = 0; i < jacobian.length; ++i) {
                for (int j = 0; j < jacobian[i].length; ++j) {
                    // check the values returned by getStateDerivatives() are correct
                    maxRelativeError = FastMath.max(maxRelativeError,
                                                    FastMath.abs((finiteDifferencesJacobian[i][j] - jacobian[i][j]) /
                                                                  finiteDifferencesJacobian[i][j]));
                }
            }
        }

        Assertions.assertEquals(0, maxRelativeError, 5.4e-6);

    }

    /**
     * Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference.
     */
    @Test
    public void testParameterDerivatives() {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // create perfect measurements
        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);
        final FDOAMeasurementCreator creator = new FDOAMeasurementCreator(context, CENTRE_FREQUENCY);
        final GroundStation primary = context.TDOAstations.getKey();
        primary.getEastOffsetDriver().setSelected(true);
        primary.getNorthOffsetDriver().setSelected(true);
        primary.getZenithOffsetDriver().setSelected(true);
        final double clockOffset = 4.8e-9;
        final GroundStation secondary = context.TDOAstations.getValue();
        secondary.getClockOffsetDriver().setValue(clockOffset);
        secondary.getClockOffsetDriver().setSelected(true);
        secondary.getEastOffsetDriver().setSelected(true);
        secondary.getNorthOffsetDriver().setSelected(true);
        secondary.getZenithOffsetDriver().setSelected(true);
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               creator,
                                                               1.0, 3.0, 300.0);
        propagator.clearStepHandlers();

        double maxRelativeError = 0;
        for (final ObservedMeasurement<?> measurement : measurements) {

            // parameter corresponding to station position offset
            final GroundStation primeParameter  = ((FDOA) measurement).getPrimeStation();
            final GroundStation secondParameter = ((FDOA) measurement).getSecondStation();

            // We intentionally propagate to a date which is close to the
            // real spacecraft state but is *not* the accurate date, by
            // compensating only part of the downlink delay. This is done
            // in order to validate the partial derivatives with respect
            // to velocity. If we had chosen the proper state date, the
            // range would have depended only on the current position but
            // not on the current velocity.
            final double          delay = measurement.getObservedValue()[0];
            final AbsoluteDate    date  = measurement.getDate().shiftedBy(delay);
            final SpacecraftState state = propagator.propagate(date);
            final ParameterDriver[] drivers = new ParameterDriver[] {
                primeParameter.getEastOffsetDriver(),
                primeParameter.getNorthOffsetDriver(),
                primeParameter.getZenithOffsetDriver(),
                secondParameter.getClockOffsetDriver(),
                secondParameter.getEastOffsetDriver(),
                secondParameter.getNorthOffsetDriver(),
                secondParameter.getZenithOffsetDriver(),
            };
            for (int i = 0; i < drivers.length; ++i) {
                final double[] gradient = measurement.estimate(0, 0, new SpacecraftState[] { state }).getParameterDerivatives(drivers[i], new AbsoluteDate());
                Assertions.assertEquals(1, measurement.getDimension());
                Assertions.assertEquals(1, gradient.length);

                final ParameterFunction dMkdP =
                                Differentiation.differentiate(new ParameterFunction() {
                                    /** {@inheritDoc} */
                                    @Override
                                    public double value(final ParameterDriver parameterDriver, AbsoluteDate date) {
                                        return measurement.estimate(0, 0, new SpacecraftState[] { state }).getEstimatedValue()[0];
                                    }
                                }, 3, 20.0 * drivers[i].getScale());
                final double ref = dMkdP.value(drivers[i], date);
                if (ref != 0.0) {
                    maxRelativeError = FastMath.max(maxRelativeError, FastMath.abs((ref - gradient[0]) / ref));
                }
            }
        }

        Assertions.assertEquals(0, maxRelativeError, 3.4e-8);

    }

}
