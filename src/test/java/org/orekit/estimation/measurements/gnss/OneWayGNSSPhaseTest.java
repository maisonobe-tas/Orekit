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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.stat.descriptive.moment.Mean;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Median;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.QuadraticClockModel;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.gnss.RadioWave;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.Differentiation;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterFunction;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedPVCoordinates;

public class OneWayGNSSPhaseTest {

    private static final RadioWave RADIO_WAVE = PredefinedGnssSignal.G01;

    /**
     * Test the values of the phase comparing the observed values and the estimated values
     * Both are calculated with a different algorithm
     */
    @Test
    public void testValues() {
        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest One-way GNSS phase Values\n");
        }
        // Run test
        this.genericTestValues(printResults);
    }

    /**
     * Test the values of the state derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    public void testStateDerivatives() {

        boolean printResults = false;
        if (printResults) {
            System.out.println("\nTest One-way GNSS phase State Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        // the following relative tolerances for derivatives with respect to velocity
        // may seem high, but they have been validated. The partial derivative of
        // signal flight time with respect to velocity ∂τ/∂{vx, vy, vz} is about 10⁻¹³
        // when the signal flight time τ is about 10⁻⁴, so finite differences lose
        // about 9 significant figures, so it is expected that partial derivatives
        // computed with finite differences will only have a few digits corrects and
        // that there will be outliers
        double refErrorsPMedian = 5.6e-09;
        double refErrorsPMean   = 2.1e-08;
        double refErrorsPMax    = 1.1e-06;
        double refErrorsVMedian = 1.7e-04;
        double refErrorsVMean   = 5.1e-04;
        double refErrorsVMax    = 4.2e-02;
        this.genericTestStateDerivatives(printResults, 0,
                                         refErrorsPMedian, refErrorsPMean, refErrorsPMax,
                                         refErrorsVMedian, refErrorsVMean, refErrorsVMax);
    }

    /**
     * Test the values of the parameters' derivatives using a numerical
     * finite differences calculation as a reference
     */
    @Test
    public void testParameterDerivatives() {

        // Print the results ?
        boolean printResults = false;

        if (printResults) {
            System.out.println("\nTest One-way GNSS phase Derivatives - Finite Differences Comparison\n");
        }
        // Run test
        double refErrorsMedian = 5.8e-15;
        double refErrorsMean   = 8.5e-15;
        double refErrorsMax    = 3.2e-14;
        this.genericTestParameterDerivatives(printResults,
                                             refErrorsMedian, refErrorsMean, refErrorsMax);

    }

    /**
     * Generic test function for values of the one-way GNSS phase
     * @param printResults Print the results ?
     */
    void genericTestValues(final boolean printResults) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect inter-satellites range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final int    ambiguity         = 1234;
        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new OneWayGNSSPhaseCreator(ephemeris, "remote",
                                                                                          RADIO_WAVE, ambiguity,
                                                                                          localClockOffset, remoteClockOffset),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> absoluteErrors = new ArrayList<>();
        final List<Double> relativeErrors = new ArrayList<>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)
                   ) {
                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState state     = interpolator.getInterpolatedState(date);

                    // Values of the phase & errors
                    final double phaseObserved  = measurement.getObservedValue()[0];
                    final EstimatedMeasurementBase<?> estimated = measurement.estimateWithoutDerivatives(new SpacecraftState[] {
                                                                                                             state,
                                                                                                             ephemeris.propagate(state.getDate())
                                                                                                         });
                    Assertions.assertEquals(RADIO_WAVE.getWavelength(), ((OneWayGNSSPhase) measurement).getWavelength(), 1.0e-15);
                    final double phaseEstimated = estimated.getEstimatedValue()[0];
                    final double absoluteError  = phaseEstimated-phaseObserved;
                    absoluteErrors.add(absoluteError);
                    relativeErrors.add(FastMath.abs(absoluteError)/FastMath.abs(phaseObserved));

                    // Print results on console ?
                    if (printResults) {
                        final AbsoluteDate measurementDate = measurement.getDate();

                        System.out.format(Locale.US, "%-23s  %-23s  %19.6f  %19.6f  %13.6e  %13.6e%n",
                                         measurementDate, date,
                                         phaseObserved, phaseEstimated,
                                         FastMath.abs(phaseEstimated-phaseObserved),
                                         FastMath.abs((phaseEstimated-phaseObserved)/phaseObserved));
                    }

                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        }); // End lambda function handlestep

        // Print results on console ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-23s  %-23s  %19s  %19s  %19s  %19s%n",
                              "Measurement Date", "State Date",
                              "Phase observed [cycles]", "Phase estimated [cycles]",
                              "ΔPhase [cycles]", "rel ΔPhase");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(Comparator.naturalOrder());

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert lists to double array
        final double[] absErrors = absoluteErrors.stream().mapToDouble(Double::doubleValue).toArray();
        final double[] relErrors = relativeErrors.stream().mapToDouble(Double::doubleValue).toArray();

        // Statistics' assertion
        final double absErrorsMedian = new Median().evaluate(absErrors);
        final double absErrorsMin    = new Min().evaluate(absErrors);
        final double absErrorsMax    = new Max().evaluate(absErrors);
        final double relErrorsMedian = new Median().evaluate(relErrors);
        final double relErrorsMax    = new Max().evaluate(relErrors);

        // Print the results on console ? Final results
        if (printResults) {
            System.out.println();
            System.out.println("Absolute errors median: " +  absErrorsMedian);
            System.out.println("Absolute errors min   : " +  absErrorsMin);
            System.out.println("Absolute errors max   : " +  absErrorsMax);
            System.out.println("Relative errors median: " +  relErrorsMedian);
            System.out.println("Relative errors max   : " +  relErrorsMax);
        }

        Assertions.assertEquals(0.0, absErrorsMedian, 6.7e-7);
        Assertions.assertEquals(0.0, absErrorsMin,    3.2e-6);
        Assertions.assertEquals(0.0, absErrorsMax,    5.7e-7);
        Assertions.assertEquals(0.0, relErrorsMedian, 5.4e-12);
        Assertions.assertEquals(0.0, relErrorsMax,    1.6e-10);

        // Test measurement type
        Assertions.assertEquals(OneWayGNSSPhase.MEASUREMENT_TYPE, measurements.get(0).getMeasurementType());
    }

    void genericTestStateDerivatives(final boolean printResults, final int index,
                                     final double refErrorsPMedian, final double refErrorsPMean, final double refErrorsPMax,
                                     final double refErrorsVMedian, final double refErrorsVMean, final double refErrorsVMax) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect one-way GNSS range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit,
                                                                                propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);

        final int    ambiguity         = 1234;
        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator,
                                                               new OneWayGNSSPhaseCreator(ephemeris, "remote",
                                                                                          RADIO_WAVE, ambiguity,
                                                                                          localClockOffset, remoteClockOffset),
                                                               1.0, 3.0, 300.0);

        // Lists for results' storage - Used only for derivatives with respect to state
        // "final" value to be seen by "handleStep" function of the propagator
        final List<Double> errorsP = new ArrayList<>();
        final List<Double> errorsV = new ArrayList<>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)
                   ) {

                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity.
                    final double            meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate      date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState[] states    = {
                        interpolator.getInterpolatedState(date),
                        ephemeris.propagate(date)
                    };
                    final double[][]      jacobian  = measurement.estimate(0, 0, states).getStateDerivatives(index);

                    // Jacobian reference value
                    final double[][] jacobianRef;

                    // Compute a reference value using finite differences
                    jacobianRef = Differentiation.differentiate(state -> {
                        final SpacecraftState[] s = states.clone();
                        s[index] = state;
                        return measurement.estimateWithoutDerivatives(s).getEstimatedValue();
                    }, measurement.getDimension(), propagator.getAttitudeProvider(),
                       OrbitType.CARTESIAN, PositionAngleType.TRUE, 8.0, 5).value(states[index]);

                    Assertions.assertEquals(jacobianRef.length, jacobian.length);
                    Assertions.assertEquals(jacobianRef[0].length, jacobian[0].length);

                    // Errors & relative errors on the Jacobian
                    double [][] dJacobian         = new double[jacobian.length][jacobian[0].length];
                    double [][] dJacobianRelative = new double[jacobian.length][jacobian[0].length];
                    for (int i = 0; i < jacobian.length; ++i) {
                        for (int j = 0; j < jacobian[i].length; ++j) {
                            dJacobian[i][j] = jacobian[i][j] - jacobianRef[i][j];
                            dJacobianRelative[i][j] = FastMath.abs(dJacobian[i][j]/jacobianRef[i][j]);

                            if (j < 3) { errorsP.add(dJacobianRelative[i][j]);
                            } else { errorsV.add(dJacobianRelative[i][j]); }
                        }
                    }
                    // Print values in console ?
                    if (printResults) {
                        System.out.format(Locale.US, "%-23s  %-23s  " +
                                        "%10.3e  %10.3e  %10.3e  " +
                                        "%10.3e  %10.3e  %10.3e  " +
                                        "%10.3e  %10.3e  %10.3e  " +
                                        "%10.3e  %10.3e  %10.3e%n",
                                        measurement.getDate(), date,
                                        dJacobian[0][0], dJacobian[0][1], dJacobian[0][2],
                                        dJacobian[0][3], dJacobian[0][4], dJacobian[0][5],
                                        dJacobianRelative[0][0], dJacobianRelative[0][1], dJacobianRelative[0][2],
                                        dJacobianRelative[0][3], dJacobianRelative[0][4], dJacobianRelative[0][5]);
                    }
                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        });

        // Print results on console ?
        if (printResults) {
            System.out.format(Locale.US, "%-23s  %-23s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s  " +
                            "%10s  %10s  %10s%n",
                            "Measurement Date", "State Date",
                            "ΔdPx", "ΔdPy", "ΔdPz", "ΔdVx", "ΔdVy", "ΔdVz",
                            "rel ΔdPx", "rel ΔdPy", "rel ΔdPz",
                            "rel ΔdVx", "rel ΔdVy", "rel ΔdVz");
        }

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements, primarily chronologically
        measurements.sort(Comparator.naturalOrder());

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert lists to double[] and evaluate some statistics
        final double[] relErrorsP = errorsP.stream().mapToDouble(Double::doubleValue).toArray();
        final double[] relErrorsV = errorsV.stream().mapToDouble(Double::doubleValue).toArray();

        final double errorsPMedian = new Median().evaluate(relErrorsP);
        final double errorsPMean   = new Mean().evaluate(relErrorsP);
        final double errorsPMax    = new Max().evaluate(relErrorsP);
        final double errorsVMedian = new Median().evaluate(relErrorsV);
        final double errorsVMean   = new Mean().evaluate(relErrorsV);
        final double errorsVMax    = new Max().evaluate(relErrorsV);

        // Print the results on console ?
        if (printResults) {
            System.out.println();
            System.out.format(Locale.US, "Relative errors dR/dP -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsPMedian, errorsPMean, errorsPMax);
            System.out.format(Locale.US, "Relative errors dR/dV -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              errorsVMedian, errorsVMean, errorsVMax);
        }

        Assertions.assertEquals(0.0, errorsPMedian, refErrorsPMedian);
        Assertions.assertEquals(0.0, errorsPMean, refErrorsPMean);
        Assertions.assertEquals(0.0, errorsPMax, refErrorsPMax);
        Assertions.assertEquals(0.0, errorsVMedian, refErrorsVMedian);
        Assertions.assertEquals(0.0, errorsVMean, refErrorsVMean);
        Assertions.assertEquals(0.0, errorsVMax, refErrorsVMax);
    }

    void genericTestParameterDerivatives(final boolean printResults,
                                         final double refErrorsMedian, final double refErrorsMean, final double refErrorsMax) {

        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        final NumericalPropagatorBuilder propagatorBuilder =
                        context.createBuilder(OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                                              1.0e-6, 60.0, 0.001);

        // Create perfect one-way GNSS range measurements
        final TimeStampedPVCoordinates original = context.initialOrbit.getPVCoordinates();
        final Orbit closeOrbit = new CartesianOrbit(new TimeStampedPVCoordinates(context.initialOrbit.getDate(),
                                                                                 original.getPosition().add(new Vector3D(1000, 2000, 3000)),
                                                                                 original.getVelocity().add(new Vector3D(-0.03, 0.01, 0.02))),
                                                    context.initialOrbit.getFrame(),
                                                    context.initialOrbit.getMu());
        final Propagator closePropagator = EstimationTestUtils.createPropagator(closeOrbit, propagatorBuilder);
        final EphemerisGenerator generator = closePropagator.getEphemerisGenerator();
        closePropagator.propagate(context.initialOrbit.getDate().shiftedBy(3.5 * closeOrbit.getKeplerianPeriod()));
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        // Create perfect phase measurements
        final int    ambiguity         = 1234;
        final double localClockOffset  = 0.137e-6;
        final double remoteClockOffset = 469.0e-6;
        final OneWayGNSSPhaseCreator creator = new OneWayGNSSPhaseCreator(ephemeris, "remote", RADIO_WAVE, ambiguity, localClockOffset, remoteClockOffset);
        creator.getLocalSatellite().getClockOffsetDriver().setSelected(true);

        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        final List<ObservedMeasurement<?>> measurements =
                        EstimationTestUtils.createMeasurements(propagator, creator, 1.0, 3.0, 300.0);

        // List to store the results
        final List<Double> relErrorList = new ArrayList<>();

        // Use a lambda function to implement "handleStep" function
        propagator.setStepHandler(interpolator -> {

            for (final ObservedMeasurement<?> measurement : measurements) {

                //  Play test if the measurement date is between interpolator previous and current date
                if ((measurement.getDate().durationFrom(interpolator.getPreviousState().getDate()) > 0.) &&
                    (measurement.getDate().durationFrom(interpolator.getCurrentState().getDate())  <=  0.)) {

                    // We intentionally propagate to a date which is close to the
                    // real spacecraft state but is *not* the accurate date, by
                    // compensating only part of the downlink delay. This is done
                    // in order to validate the partial derivatives with respect
                    // to velocity. If we had chosen the proper state date, the
                    // phase would have depended only on the current position but
                    // not on the current velocity.
                    final double          meanDelay = measurement.getObservedValue()[0] / Constants.SPEED_OF_LIGHT;
                    final AbsoluteDate    date      = measurement.getDate().shiftedBy(-0.75 * meanDelay);
                    final SpacecraftState[] states    = {
                        interpolator.getInterpolatedState(date),
                        ephemeris.propagate(date)
                    };
                    final ParameterDriver[] drivers = new ParameterDriver[] {
                        measurement.getSatellites().get(0).getClockOffsetDriver(),
                    };

                    for (int i = 0; i < drivers.length; ++i) {
                        for (Span<String> span = drivers[i].getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                            final double[] gradient  = measurement.estimate(0, 0, states).getParameterDerivatives(drivers[i], span.getStart());
                            Assertions.assertEquals(1, measurement.getDimension());
                            Assertions.assertEquals(1, gradient.length);
                            
                            // Compute a reference value using finite differences
                            final ParameterFunction dMkdP =
                                            Differentiation.differentiate(new ParameterFunction() {
                                                /** {@inheritDoc} */
                                                @Override
                                                public double value(final ParameterDriver parameterDriver, final AbsoluteDate date) {
                                                    return measurement.
                                                           estimateWithoutDerivatives(states).
                                                           getEstimatedValue()[0];
                                                }
                                            }, 5, 10.0 * drivers[i].getScale());
                            final double ref = dMkdP.value(drivers[i], date);
                            
                            if (printResults) {
                                System.out.format(Locale.US, "%10.3e  %10.3e  ", gradient[0]-ref, FastMath.abs((gradient[0]-ref)/ref));
                            }
                            
                            final double relError = FastMath.abs((ref-gradient[0])/ref);
                            relErrorList.add(relError);
                        }
                    }
                    if (printResults) {
                        System.out.format(Locale.US, "%n");
                    }

                } // End if measurement date between previous and current interpolator step
            } // End for loop on the measurements
        });

        // Rewind the propagator to initial date
        propagator.propagate(context.initialOrbit.getDate());

        // Sort measurements chronologically
        measurements.sort(Comparator.naturalOrder());

        // Check number of parameter drivers attached to measurement
        List<ParameterDriver> paramDrivers = measurements.get(0).getParametersDrivers();
        Assertions.assertEquals(4, paramDrivers.size());

        // Check the parameter names
        Assertions.assertEquals("clock-offset-sat-0", paramDrivers.get(0).getName());
        Assertions.assertEquals("clock-drift-sat-0", paramDrivers.get(1).getName());
        Assertions.assertEquals("clock-acceleration-sat-0", paramDrivers.get(2).getName());
        Assertions.assertEquals("ambiguity-remote-sat-0-154.00", paramDrivers.get(3).getName());

        // Print results ? Header
        if (printResults) {
            System.out.format(Locale.US, "%-15s  %-23s  %-23s  " +
                              "%10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s  %10s%n",
                              "Station", "Measurement Date", "State Date",
                              "Δt",    "rel Δt",
                              "ΔdQx",  "rel ΔdQx",
                              "ΔdQy",  "rel ΔdQy",
                              "ΔdQz",  "rel ΔdQz",
                              "Δtsat", "rel Δtsat");
         }

        // Propagate to final measurement's date
        propagator.propagate(measurements.get(measurements.size()-1).getDate());

        // Convert error list to double[]
        final double[] relErrors = relErrorList.stream().mapToDouble(Double::doubleValue).toArray();

        // Compute statistics
        final double relErrorsMedian = new Median().evaluate(relErrors);
        final double relErrorsMean   = new Mean().evaluate(relErrors);
        final double relErrorsMax    = new Max().evaluate(relErrors);

        // Print the results on console ?
        if (printResults) {
            System.out.println();
            System.out.format(Locale.US, "Relative errors dR/dQ -> Median: %6.3e / Mean: %6.3e / Max: %6.3e%n",
                              relErrorsMedian, relErrorsMean, relErrorsMax);
        }

        Assertions.assertEquals(0.0, relErrorsMedian, refErrorsMedian);
        Assertions.assertEquals(0.0, relErrorsMean, refErrorsMean);
        Assertions.assertEquals(0.0, relErrorsMax, refErrorsMax);

    }

    @Test
    public void testIssue734() {

        Utils.setDataRoot("regular-data");

        // Create a phase measurement. Remote is set to null since it not used by the test
        final OneWayGNSSPhase phase = new OneWayGNSSPhase(null, "",
                                                          new QuadraticClockModel(AbsoluteDate.J2000_EPOCH, 635.0e-6, 0.0, 0.0),
                                                          AbsoluteDate.J2000_EPOCH, 467614.701,
                                                          PredefinedGnssSignal.G01.getWavelength(), 0.02, 1.0,
                                                          new ObservableSatellite(0),
                                                          new AmbiguityCache());

        // First check
        Assertions.assertEquals(0.0, phase.getAmbiguityDriver().getValue(), Double.MIN_VALUE);
        Assertions.assertFalse(phase.getAmbiguityDriver().isSelected());

        // Perform some changes in ambiguity driver
        phase.getAmbiguityDriver().setValue(1234.0);
        phase.getAmbiguityDriver().setSelected(true);

        // Second check
        Assertions.assertEquals(1234.0, phase.getAmbiguityDriver().getValue(), Double.MIN_VALUE);
        Assertions.assertTrue(phase.getAmbiguityDriver().isSelected());
        for (ParameterDriver driver : phase.getParametersDrivers()) {
            // Verify if the current driver corresponds to the phase ambiguity
            if (driver instanceof AmbiguityDriver) {
                Assertions.assertEquals(1234.0, phase.getAmbiguityDriver().getValue(), Double.MIN_VALUE);
                Assertions.assertTrue(phase.getAmbiguityDriver().isSelected());
            }
        }

    }

}
