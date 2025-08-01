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
package org.orekit.propagation.numerical;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.ode.nonstiff.LutherIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.*;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.*;
import org.orekit.propagation.*;
import org.orekit.propagation.conversion.DormandPrince853IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.events.*;
import org.orekit.propagation.events.handlers.*;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.*;
import org.orekit.utils.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class NumericalPropagatorTest {

    private double               mu;
    private AbsoluteDate         initDate;
    private SpacecraftState      initialState;
    private NumericalPropagator  propagator;

    @Test
    void testDependsOnTimeOnlyWrong() {
        // GIVEN
        final AdditionalDataProvider<Boolean> dummyDataProvider = new AdditionalDataProvider<Boolean>() {
            @Override
            public String getName() {
                return "dummy";
            }
            @Override
            public Boolean getAdditionalData(SpacecraftState state) {
                return Boolean.TRUE;
            }
        };
        propagator.addAdditionalDataProvider(dummyDataProvider);
        propagator.setInitialState(initialState.addAdditionalData(dummyDataProvider.getName(), Boolean.TRUE));
        final EventDetector detectorCallingData = new DateDetector() {
            @Override
            public double g(SpacecraftState s) {
                s.getAdditionalData(dummyDataProvider.getName());  // this will cause an exception since detector should not depend on additional variables
                return super.g(s);
            }
        };
        // WHEN & THEN
        propagator.addEventDetector(detectorCallingData);
        final AbsoluteDate targetDate = initialState.getDate().shiftedBy(1);
        Assertions.assertThrows(OrekitException.class, () -> propagator.propagate(targetDate));
    }

    @Test
    void testIssue1032() {
        Assertions.assertEquals(PropagationType.OSCULATING, propagator.getPropagationType());
    }

    @Test
    void testPropagateWithNonResettableManeuver() {
        final ManeuverTriggers triggers = new ManeuverTriggers() {
            @Override
            public boolean isFiring(AbsoluteDate date, double[] parameters) {
                return false;
            }

            @Override
            public <T extends CalculusFieldElement<T>> boolean isFiring(FieldAbsoluteDate<T> date, T[] parameters) {
                return false;
            }

            @Override
            public Stream<EventDetector> getEventDetectors() {
                return Stream.empty();
            }

            @Override
            public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
                return Stream.empty();
            }

            @Override
            public List<ParameterDriver> getParametersDrivers() {
                return Collections.emptyList();
            }
        };
        propagator.addForceModel(new Maneuver(null, triggers, new BasicConstantThrustPropulsionModel(0., 1., Vector3D.PLUS_I, "")));
        propagator.setupMatricesComputation("stm", MatrixUtils.createRealIdentityMatrix(6), new DoubleArrayDictionary());
        propagator.setUpStmAndJacobianGenerators();
    }

    @Test
    void testIssue879() {
        // GIVEN
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        // WHEN
        propagator.clearEphemerisGenerators();
        propagator.propagate(initDate.shiftedBy(1));
        // THEN
        Assertions.assertThrows(NullPointerException.class, generator::getGeneratedEphemeris);
    }

    @Test
    void testEventsWithTimeRangePropagation() {
        final AtomicInteger counter = new AtomicInteger(0);
        final double dt = 60.0;
        final EventDetector singleDetector = new DateDetector(initDate.shiftedBy(dt / 2)).
                                             withHandler((state, detector, increasing) -> {
                                                 counter.incrementAndGet();
                                                 return Action.CONTINUE;
                                             });
        ForceModel force = new ForceModelAdapter() {
            @Override
            public Stream<EventDetector> getEventDetectors() {
                return Stream.of(singleDetector);
            }
        };

        // action
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(force);
        AbsoluteDate target = initDate.shiftedBy(dt);
        propagator.propagate(initDate.shiftedBy(1.0), target);

        Assertions.assertEquals(1, counter.intValue());

    }

    @Test
    void testForceModelInitialized() {
        // setup
        // mutable holders
        SpacecraftState[] actualState = new SpacecraftState[1];
        AbsoluteDate[] actualDate = new AbsoluteDate[1];
        ForceModel force = new ForceModelAdapter() {
            @Override
            public void init(SpacecraftState initialState, AbsoluteDate target) {
                actualState[0] = initialState;
                actualDate[0] = target;
            }
        };

        // action
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(force);
        AbsoluteDate target = initDate.shiftedBy(60);
        propagator.propagate(target);

        // verify
        MatcherAssert.assertThat(actualDate[0], CoreMatchers.is(target));
        MatcherAssert.assertThat(actualState[0].getDate().durationFrom(initDate),
                CoreMatchers.is(0.0));
        MatcherAssert.assertThat(actualState[0].getPVCoordinates(),
                OrekitMatchers.pvIs(initialState.getPVCoordinates()));
    }

    @Test
    @Deprecated
    void testTolerancesOrbitdV() {
        // GIVEN
        final double dP = 1e-3;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final Orbit orbit = new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, position,
                velocity, Vector3D.ZERO), FramesFactory.getGCRF(), Constants.EGM96_EARTH_MU);
        // WHEN
        final double[][] tolerancesWithDv = NumericalPropagator.tolerances(dP, 1e-6, orbit, OrbitType.CARTESIAN);
        // THEN
        final double[][] tolerances = ToleranceProvider.getDefaultToleranceProvider(dP).getTolerances(orbit, OrbitType.CARTESIAN);
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(tolerances[0][i], tolerancesWithDv[0][i]);
        }
    }

    @Deprecated
    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testTolerancesOrbit(final OrbitType orbitType) {
        // GIVEN
        final double dP = 1e-3;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final Orbit orbit = new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, position,
                velocity, Vector3D.ZERO), FramesFactory.getGCRF(), Constants.EGM96_EARTH_MU);
        // WHEN
        final double[][] actualTolerances = NumericalPropagator.tolerances(dP, orbit, orbitType);
        // THEN
        final double[][] expectedTolerances = ToleranceProvider.getDefaultToleranceProvider(dP).getTolerances(orbit, orbitType);
        Assertions.assertArrayEquals(expectedTolerances[0], actualTolerances[0]);
        Assertions.assertArrayEquals(expectedTolerances[1], actualTolerances[1]);
    }

    @Deprecated
    @Test
    void testTolerances() {
        // GIVEN
        final double dP = 1e-3;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        final Orbit orbit = new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, position,
                velocity, Vector3D.ZERO), FramesFactory.getGCRF(), Constants.EGM96_EARTH_MU);
        // WHEN
        final double[][] orbitTolerances = NumericalPropagator.tolerances(dP, orbit, OrbitType.CARTESIAN);
        // THEN
        final double[][] pvTolerances = ToleranceProvider.getDefaultToleranceProvider(dP).getTolerances(new AbsolutePVCoordinates(orbit.getFrame(),
                new TimeStampedPVCoordinates(orbit.getDate(), position, velocity)));
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(pvTolerances[0][i], orbitTolerances[0][i]);
            Assertions.assertEquals(pvTolerances[1][i], orbitTolerances[1][i]);
        }
    }

    @Test
    void testEphemerisModeWithHandler() {
        // setup
        AbsoluteDate end = initDate.shiftedBy(90 * 60);

        // action
        final List<SpacecraftState> states = new ArrayList<>();
        EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.setStepHandler(interpolator -> states.add(interpolator.getCurrentState()));
        propagator.propagate(end);
        final BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        //verify
        Assertions.assertTrue(states.size() > 10); // got some data
        for (SpacecraftState state : states) {
            PVCoordinates actual =
                    ephemeris.propagate(state.getDate()).getPVCoordinates();
            MatcherAssert.assertThat(actual, OrekitMatchers.pvIs(state.getPVCoordinates()));
        }
    }

    /** test for issue #238 */
    @Test
    void testEventAtEndOfEphemeris() {
        // setup
        // choose duration that will round up when expressed as a double
        AbsoluteDate end = initDate.shiftedBy(100)
                .shiftedBy(3 * FastMath.ulp(100.0) / 4);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(end);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        CountAndContinue handler = new CountAndContinue();
        DateDetector detector = new DateDetector(end).
                                withMaxCheck(10).
                                withThreshold(1e-9).
                                withMinGap(10).
                                withHandler(handler);
        // propagation works fine w/o event detector, but breaks with it
        ephemeris.addEventDetector(detector);

        //action
        // fails when this throws an "out of range date for ephemerides"
        SpacecraftState actual = ephemeris.propagate(end);

        //verify
        Assertions.assertEquals(actual.getDate().durationFrom(end), 0.0, 0.0);
        Assertions.assertEquals(1, handler.getCount());
    }

    /** test for issue #238 */
    @Test
    void testEventAtBeginningOfEphemeris() {
        // setup
        // choose duration that will round up when expressed as a double
        AbsoluteDate end = initDate.shiftedBy(100)
                .shiftedBy(3 * FastMath.ulp(100.0) / 4);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(end);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        CountAndContinue handler = new CountAndContinue();
        // events directly on propagation start date are not triggered,
        // so move the event date slightly after
        AbsoluteDate eventDate = initDate.shiftedBy(FastMath.ulp(100.0) / 10.0);
        DateDetector detector = new DateDetector(eventDate).
                                withMaxCheck(10).
                                withThreshold(1e-9).
                                withMinGap(10).
                                withHandler(handler);
        // propagation works fine w/o event detector, but breaks with it
        ephemeris.addEventDetector(detector);

        // action + verify
        // propagate forward
        Assertions.assertEquals(ephemeris.propagate(end).getDate().durationFrom(end), 0.0, 0.0);
        // propagate backward
        Assertions.assertEquals(ephemeris.propagate(initDate).getDate().durationFrom(initDate), 0.0, 0.0);
        Assertions.assertEquals(2, handler.getCount());
    }

    /**
     * check propagation succeeds when two events are within the tolerance of
     * each other.
     */
    @Test
    void testCloseEventDates() {
        // setup
        DateDetector d1 = new DateDetector(initDate.shiftedBy(15)).
                          withMaxCheck(10).
                          withThreshold(1).
                          withHandler(new ContinueOnEvent());
        DateDetector d2 = new DateDetector(initDate.shiftedBy(15.5)).
                          withMaxCheck(10).
                          withThreshold(1).
                          withHandler(new ContinueOnEvent());
        propagator.addEventDetector(d1);
        propagator.addEventDetector(d2);

        //action
        AbsoluteDate end = initDate.shiftedBy(30);
        SpacecraftState actual = propagator.propagate(end);

        //verify
        Assertions.assertEquals(actual.getDate().durationFrom(end), 0.0, 0.0);
    }

    @Test
    void testEphemerisDates() {
        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate initialDate = new AbsoluteDate("2015-07-01", tai);
        AbsoluteDate startDate = new AbsoluteDate("2015-07-03", tai).shiftedBy(-0.1);
        AbsoluteDate endDate = new AbsoluteDate("2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        KeplerianOrbit orbit = new KeplerianOrbit(
                600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0, 0, 0, 0,
                PositionAngleType.TRUE, eci, initialDate, mu);
        OrbitType type = OrbitType.CARTESIAN;
        double[][] tol = ToleranceProvider.of(CartesianToleranceProvider.of(1e-3)).getTolerances(orbit, type, NumericalPropagator.DEFAULT_POSITION_ANGLE_TYPE);
        NumericalPropagator prop = new NumericalPropagator(
                new DormandPrince853Integrator(0.1, 500, tol[0], tol[1]));
        prop.setOrbitType(type);
        prop.resetInitialState(new SpacecraftState(new CartesianOrbit(orbit)));

        //action
        final EphemerisGenerator generator = prop.getEphemerisGenerator();
        prop.propagate(startDate, endDate);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        //verify
        TimeStampedPVCoordinates actualPV = ephemeris.getPVCoordinates(startDate, eci);
        TimeStampedPVCoordinates expectedPV = orbit.getPVCoordinates(startDate, eci);
        MatcherAssert.assertThat(actualPV.getPosition(),
                OrekitMatchers.vectorCloseTo(expectedPV.getPosition(), 1.0));
        MatcherAssert.assertThat(actualPV.getVelocity(),
                OrekitMatchers.vectorCloseTo(expectedPV.getVelocity(), 1.0));
        MatcherAssert.assertThat(ephemeris.getMinDate().durationFrom(startDate),
                OrekitMatchers.closeTo(0, 0));
        MatcherAssert.assertThat(ephemeris.getMaxDate().durationFrom(endDate),
                OrekitMatchers.closeTo(0, 0));
        //test date
        AbsoluteDate date = endDate.shiftedBy(-0.11);
        Assertions.assertEquals(
                ephemeris.propagate(date).getDate().durationFrom(date), 0, 0);
    }

    @Test
    void testEphemerisDatesBackward() {
        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate initialDate = new AbsoluteDate("2015-07-05", tai);
        AbsoluteDate startDate = new AbsoluteDate("2015-07-03", tai).shiftedBy(-0.1);
        AbsoluteDate endDate = new AbsoluteDate("2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        KeplerianOrbit orbit = new KeplerianOrbit(
                600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0, 0, 0, 0,
                PositionAngleType.TRUE, eci, initialDate, mu);
        OrbitType type = OrbitType.CARTESIAN;
        double[][] tol = ToleranceProvider.of(CartesianToleranceProvider.of(1e-3)).getTolerances(orbit, type);
        NumericalPropagator prop = new NumericalPropagator(
                new DormandPrince853Integrator(0.1, 500, tol[0], tol[1]));
        prop.setOrbitType(type);
        prop.resetInitialState(new SpacecraftState(new CartesianOrbit(orbit)));

        //action
        final EphemerisGenerator generator = prop.getEphemerisGenerator();
        prop.propagate(endDate, startDate);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        //verify
        TimeStampedPVCoordinates actualPV = ephemeris.getPVCoordinates(startDate, eci);
        TimeStampedPVCoordinates expectedPV = orbit.getPVCoordinates(startDate, eci);
        MatcherAssert.assertThat(actualPV.getPosition(),
                OrekitMatchers.vectorCloseTo(expectedPV.getPosition(), 1.0));
        MatcherAssert.assertThat(actualPV.getVelocity(),
                OrekitMatchers.vectorCloseTo(expectedPV.getVelocity(), 1.0));
        MatcherAssert.assertThat(ephemeris.getMinDate().durationFrom(startDate),
                OrekitMatchers.closeTo(0, 0));
        MatcherAssert.assertThat(ephemeris.getMaxDate().durationFrom(endDate),
                OrekitMatchers.closeTo(0, 0));
        //test date
        AbsoluteDate date = endDate.shiftedBy(-0.11);
        Assertions.assertEquals(
                ephemeris.propagate(date).getDate().durationFrom(date), 0, 0);
    }

    @Test
    void testNoExtrapolation() {

        // Propagate of the initial at the initial date
        final SpacecraftState finalState = propagator.propagate(initDate);

        // Initial orbit definition
        final Vector3D initialPosition = initialState.getPosition();
        final Vector3D initialVelocity = initialState.getVelocity();

        // Final orbit definition
        final Vector3D finalPosition   = finalState.getPosition();
        final Vector3D finalVelocity   = finalState.getVelocity();

        // Check results
        Assertions.assertEquals(initialPosition.getX(), finalPosition.getX(), 1.0e-10);
        Assertions.assertEquals(initialPosition.getY(), finalPosition.getY(), 1.0e-10);
        Assertions.assertEquals(initialPosition.getZ(), finalPosition.getZ(), 1.0e-10);
        Assertions.assertEquals(initialVelocity.getX(), finalVelocity.getX(), 1.0e-10);
        Assertions.assertEquals(initialVelocity.getY(), finalVelocity.getY(), 1.0e-10);
        Assertions.assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 1.0e-10);

    }

    @Test
    void testNotInitialised1() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final AbstractIntegratedPropagator notInitialised =
                    new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.0));
            notInitialised.propagate(AbsoluteDate.J2000_EPOCH);
        });
    }

    @Test
    void testNotInitialised2() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final AbstractIntegratedPropagator notInitialised =
                    new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.0));
            notInitialised.propagate(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH.shiftedBy(3600));
        });
    }

    @Test
    void testKepler() {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        final Orbit finalOrbit =
            propagator.propagate(initDate.shiftedBy(-60), initDate.shiftedBy(dt)).getOrbit();

        // Check results
        final Orbit initialOrbit = initialState.getOrbit();
        final double n = FastMath.sqrt(initialOrbit.getMu() / initialOrbit.getA()) / initialOrbit.getA();
        Assertions.assertEquals(initialOrbit.getA(),    finalOrbit.getA(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getEquinoctialEx(),    finalOrbit.getEquinoctialEx(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getEquinoctialEy(),    finalOrbit.getEquinoctialEy(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getHx(),    finalOrbit.getHx(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getHy(),    finalOrbit.getHy(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getLM() + n * dt, finalOrbit.getLM(), 2.0e-9);

    }

    @Test
    void testCartesian() {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        propagator.setOrbitType(OrbitType.CARTESIAN);
        final PVCoordinates finalState =
            propagator.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        final Vector3D pFin = finalState.getPosition();
        final Vector3D vFin = finalState.getVelocity();

        // Check results
        final PVCoordinates reference = initialState.shiftedBy(dt).getPVCoordinates();
        final Vector3D pRef = reference.getPosition();
        final Vector3D vRef = reference.getVelocity();
        Assertions.assertEquals(0, pRef.subtract(pFin).getNorm(), 2e-4);
        Assertions.assertEquals(0, vRef.subtract(vFin).getNorm(), 7e-8);

    }

    @Test
    void testPropagationTypesElliptical() throws ParseException, IOException {
     // setup
        AbsoluteDate         initDate  = new AbsoluteDate();
        SpacecraftState     initialState;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;

        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = ToleranceProvider.of(CartesianToleranceProvider.of(0.001)).getTolerances(orbit, type);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                  GravityFieldFactory.getNormalizedProvider(5, 5));
        propagator.addForceModel(gravityField);

        // Propagation of the initial at t + dt
        final PVCoordinates pv = initialState.getPVCoordinates();
        final double dP = 0.001;
        final double dV = initialState.getOrbit().getMu() * dP /
                          (pv.getPosition().getNormSq() * pv.getVelocity().getNorm());

        final PVCoordinates pvcM = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngleType.MEAN);
        final PVCoordinates pviM = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngleType.MEAN);
        final PVCoordinates pveM = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngleType.MEAN);
        final PVCoordinates pvkM = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngleType.MEAN);

        final PVCoordinates pvcE = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngleType.ECCENTRIC);
        final PVCoordinates pviE = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngleType.ECCENTRIC);
        final PVCoordinates pveE = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngleType.ECCENTRIC);
        final PVCoordinates pvkE = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngleType.ECCENTRIC);

        final PVCoordinates pvcT = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngleType.TRUE);
        final PVCoordinates pviT = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngleType.TRUE);
        final PVCoordinates pveT = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngleType.TRUE);
        final PVCoordinates pvkT = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngleType.TRUE);

        Assertions.assertEquals(0, pvcM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 3.1);
        Assertions.assertEquals(0, pvcM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2.1);
        Assertions.assertEquals(0, pviM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.8);
        Assertions.assertEquals(0, pviM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.5);
        Assertions.assertEquals(0, pvkM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.5);
        Assertions.assertEquals(0, pvkM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.4);
        Assertions.assertEquals(0, pveM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.3);
        Assertions.assertEquals(0, pveM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.3);

        Assertions.assertEquals(0, pvcE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 3.1);
        Assertions.assertEquals(0, pvcE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2.1);
        Assertions.assertEquals(0, pviE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.1);
        Assertions.assertEquals(0, pviE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.09);
        Assertions.assertEquals(0, pvkE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.5);
        Assertions.assertEquals(0, pvkE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.3);
        Assertions.assertEquals(0, pveE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.2);
        Assertions.assertEquals(0, pveE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.2);

        Assertions.assertEquals(0, pvcT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 3.1);
        Assertions.assertEquals(0, pvcT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2.1);
        Assertions.assertEquals(0, pviT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.4);
        Assertions.assertEquals(0, pviT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.2);
        Assertions.assertEquals(0, pvkT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.5);
        Assertions.assertEquals(0, pvkT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.2);

    }

    @Test
    void testPropagationTypesHyperbolic() throws ParseException, IOException {

        SpacecraftState state =
            new SpacecraftState(new KeplerianOrbit(-10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                   PositionAngleType.TRUE,
                                                   FramesFactory.getEME2000(), initDate,
                                                   mu));

        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                  GravityFieldFactory.getNormalizedProvider(5, 5));
        propagator.addForceModel(gravityField);

        // Propagation of the initial at t + dt
        final PVCoordinates pv = state.getPVCoordinates();
        final double dP = 0.001;
        final double dV = state.getOrbit().getMu() * dP /
                          (pv.getPosition().getNormSq() * pv.getVelocity().getNorm());

        final PVCoordinates pvcM = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngleType.MEAN);
        final PVCoordinates pvkM = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngleType.MEAN);

        final PVCoordinates pvcE = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngleType.ECCENTRIC);
        final PVCoordinates pvkE = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngleType.ECCENTRIC);

        final PVCoordinates pvcT = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngleType.TRUE);
        final PVCoordinates pvkT = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngleType.TRUE);

        Assertions.assertEquals(0, pvcM.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.4);
        Assertions.assertEquals(0, pvcM.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.6);
        Assertions.assertEquals(0, pvkM.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.4);
        Assertions.assertEquals(0, pvkM.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.6);

        Assertions.assertEquals(0, pvcE.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.3);
        Assertions.assertEquals(0, pvcE.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.6);
        Assertions.assertEquals(0, pvkE.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.1);
        Assertions.assertEquals(0, pvkE.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.04);

        Assertions.assertEquals(0, pvcT.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.3);
        Assertions.assertEquals(0, pvcT.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.6);

    }

    private PVCoordinates propagateInType(SpacecraftState state, double dP,
                                          OrbitType type, PositionAngleType angle)
        {

        final double dt = 3200;
        final double minStep = 0.001;
        final double maxStep = 1000;

        double[][] tol = ToleranceProvider.of(CartesianToleranceProvider.of(dP)).getTolerances(state.getOrbit(), type,
                angle);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        NumericalPropagator newPropagator = new NumericalPropagator(integrator);
        newPropagator.setOrbitType(type);
        newPropagator.setPositionAngleType(angle);
        newPropagator.setInitialState(state);
        for (ForceModel force: propagator.getAllForceModels()) {
            newPropagator.addForceModel(force);
        }
        return newPropagator.propagate(state.getDate().shiftedBy(dt)).getPVCoordinates();

    }

    @Test
    void testException() {
        Assertions.assertThrows(OrekitException.class, () -> {
            propagator.setStepHandler(new OrekitStepHandler() {
                private int countDown = 3;
                private AbsoluteDate previousCall = null;
                public void init(SpacecraftState s0, AbsoluteDate t) {
                }
                public void handleStep(OrekitStepInterpolator interpolator) {
                    if (previousCall != null) {
                        Assertions.assertTrue(interpolator.getCurrentState().getDate().compareTo(previousCall) < 0);
                    }
                    if (--countDown == 0) {
                        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE, "dummy error");
                    }
                }
            });
            propagator.propagate(initDate.shiftedBy(-3600));
        });
    }

    @Test
    void testStopEvent() {
        final AbsoluteDate stopDate = initDate.shiftedBy(1000);
        CheckingHandler checking = new CheckingHandler(Action.STOP);
        propagator.addEventDetector(new DateDetector(stopDate).withHandler(checking));
        Assertions.assertEquals(1, propagator.getEventDetectors().size());
        checking.assertEvent(false);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(3200));
        checking.assertEvent(true);
        Assertions.assertEquals(0, finalState.getDate().durationFrom(stopDate), 1.0e-10);
        propagator.clearEventsDetectors();
        Assertions.assertEquals(0, propagator.getEventDetectors().size());
        Assertions.assertTrue(checking.isFinished);
    }

    @Test
    void testResetStateEvent() {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        CheckingHandler checking = new CheckingHandler(Action.RESET_STATE) {
            public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
                return new SpacecraftState(oldState.getOrbit(), oldState.getAttitude()).withMass(oldState.getMass() - 200.0);
            }
        };
        propagator.addEventDetector(new DateDetector(resetDate).withHandler(checking));
        checking.assertEvent(false);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(3200));
        checking.assertEvent(true);
        Assertions.assertEquals(initialState.getMass() - 200, finalState.getMass(), 1.0e-10);
    }

    @Test
    void testResetDerivativesEvent() {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        CheckingHandler checking = new CheckingHandler(Action.RESET_DERIVATIVES);
        propagator.addEventDetector(new DateDetector(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        Assertions.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate), 1.0e-10);
        propagator.setResetAtEnd(true);
        final Orbit finalOrbit =
            propagator.propagate(initDate.shiftedBy(dt)).getOrbit();
        Assertions.assertEquals(dt, propagator.getInitialState().getDate().durationFrom(initDate), 1.0e-10);
        checking.assertEvent(true);
        final Orbit initialOrbit = initialState.getOrbit();
        final double n = FastMath.sqrt(initialOrbit.getMu() / initialOrbit.getA()) / initialOrbit.getA();
        Assertions.assertEquals(initialOrbit.getA(),    finalOrbit.getA(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getEquinoctialEx(),    finalOrbit.getEquinoctialEx(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getEquinoctialEy(),    finalOrbit.getEquinoctialEy(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getHx(),    finalOrbit.getHx(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getHy(),    finalOrbit.getHy(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getLM() + n * dt, finalOrbit.getLM(), 6.0e-10);
    }

    @Test
    void testContinueEvent() {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        CheckingHandler checking = new CheckingHandler(Action.CONTINUE);
        propagator.addEventDetector(new DateDetector(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        Assertions.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate), 1.0e-10);
        propagator.setResetAtEnd(false);
        final Orbit finalOrbit =
            propagator.propagate(initDate.shiftedBy(dt)).getOrbit();
        Assertions.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate), 1.0e-10);
        checking.assertEvent(true);
        final Orbit initialOrbit = initialState.getOrbit();
        final double n = FastMath.sqrt(initialOrbit.getMu() / initialOrbit.getA()) / initialOrbit.getA();
        Assertions.assertEquals(initialOrbit.getA(),    finalOrbit.getA(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getEquinoctialEx(),    finalOrbit.getEquinoctialEx(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getEquinoctialEy(),    finalOrbit.getEquinoctialEy(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getHx(),    finalOrbit.getHx(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getHy(),    finalOrbit.getHy(),    1.0e-10);
        Assertions.assertEquals(initialOrbit.getLM() + n * dt, finalOrbit.getLM(), 6.0e-10);
    }

    @Test
    void testAdditionalStateEvent() {
        propagator.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {

            public String getName() {
                return "linear";
            }

            public int getDimension() {
                return 1;
            }

            public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                return new CombinedDerivatives(new double[] { 1.0 }, null);
            }

        });
        try {
            propagator.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {

                public String getName() {
                    return "linear";
                }

                public int getDimension() {
                    return 1;
                }

                public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                    return new CombinedDerivatives(new double[] { 1.0 }, null);
                }

            });
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE);
        }
        try {
            propagator.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {
               public String getName() {
                    return "linear";
                }

               public int getDimension() {
                   return 1;
               }

               public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                   return new CombinedDerivatives(new double[1], null);
               }
            });
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(oe.getSpecifier(), OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE);
        }
        propagator.addAdditionalDataProvider(new AdditionalDataProvider<Double>() {
            public String getName() {
                return "constant";
            }

            public Double getAdditionalData(SpacecraftState state) {
                return 1.0;
            }
        });
        Assertions.assertTrue(propagator.isAdditionalDataManaged("linear"));
        Assertions.assertTrue(propagator.isAdditionalDataManaged("constant"));
        Assertions.assertFalse(propagator.isAdditionalDataManaged("non-managed"));
        Assertions.assertEquals(2, propagator.getManagedAdditionalData().length);
        propagator.setInitialState(propagator.getInitialState().addAdditionalData("linear", 1.5));

        CheckingHandler checking = new CheckingHandler(Action.STOP);
        propagator.addEventDetector(new AdditionalStateLinearDetector(new EventDetectionSettings(10.0, 1.0e-8, EventDetectionSettings.DEFAULT_MAX_ITER),
                checking));

        final double dt = 3200;
        checking.assertEvent(false);
        final SpacecraftState finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        checking.assertEvent(true);
        Assertions.assertEquals(3.0, finalState.getAdditionalState("linear")[0], 1.0e-8);
        Assertions.assertEquals(1.5, finalState.getDate().durationFrom(initDate), 1.0e-8);

    }

    private static class AdditionalStateLinearDetector implements EventDetector {

        private final EventHandler eventHandler;
        private final EventDetectionSettings detectionSettings;

        AdditionalStateLinearDetector(final EventDetectionSettings detectionSettings,
                                      final EventHandler eventHandler) {
            this.detectionSettings = detectionSettings;
            this.eventHandler = eventHandler;
        }

        public double g(SpacecraftState s) {
            return s.getAdditionalState("linear")[0] - 3.0;
        }

        @Override
        public EventDetectionSettings getDetectionSettings() {
            return detectionSettings;
        }

        @Override
        public EventHandler getHandler() {
            return eventHandler;
        }

    }

    @Test
    void testResetAdditionalStateEvent() {
        propagator.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {

            public String getName() {
                return "linear";
            }

            public int getDimension() {
                return 1;
            }

            public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                return new CombinedDerivatives(new double[] { 1.0 }, null);
            }
        });
        propagator.setInitialState(propagator.getInitialState().addAdditionalData("linear", 1.5));

        CheckingHandler checking = new CheckingHandler(Action.RESET_STATE) {
            public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState)
                {
                return oldState.addAdditionalData("linear", oldState.getAdditionalState("linear")[0] * 2);
            }
        };

        propagator.addEventDetector(new AdditionalStateLinearDetector(new EventDetectionSettings(10.0, 1.0e-8, EventDetectionSettings.DEFAULT_MAX_ITER),
                checking));

        final double dt = 3200;
        checking.assertEvent(false);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(dt));
        checking.assertEvent(true);
        Assertions.assertEquals(dt + 4.5, finalState.getAdditionalState("linear")[0], 1.0e-8);
        Assertions.assertEquals(dt, finalState.getDate().durationFrom(initDate), 1.0e-8);

    }

    @Test
    void testEventDetectionBug() throws IOException, ParseException {

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate = new AbsoluteDate(2005, 1, 1, 0, 0, 0.0, utc);
        double duration = 100000.;
        AbsoluteDate endDate = new AbsoluteDate(initialDate, duration);

        // Initialization of the frame EME2000
        Frame EME2000 = FramesFactory.getEME2000();


        // Initial orbit
        double a = 35786000. + 6378137.0;
        double e = 0.70;
        double rApogee = a*(1+e);
        double vApogee = FastMath.sqrt(mu*(1-e)/(a*(1+e)));
        Orbit geo = new CartesianOrbit(new PVCoordinates(new Vector3D(rApogee, 0., 0.),
                                                         new Vector3D(0., vApogee, 0.)), EME2000,
                                                         initialDate, mu);


        duration = geo.getKeplerianPeriod();
        endDate = new AbsoluteDate(initialDate, duration);

        // Numerical Integration
        final double minStep  = 0.001;
        final double maxStep  = 1000;
        final double initStep = 60;
        final double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7};

        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(minStep, maxStep, absTolerance, relTolerance);
        integrator.setInitialStepSize(initStep);

        // Numerical propagator based on the integrator
        propagator = new NumericalPropagator(integrator);
        double mass = 1000.;
        SpacecraftState initialState = new SpacecraftState(geo).withMass(mass);
        propagator.setInitialState(initialState);
        propagator.setOrbitType(OrbitType.CARTESIAN);


        // Set the events Detectors
        ApsideDetector event1 = new ApsideDetector(geo);
        propagator.addEventDetector(event1);

        // Set the propagation mode
        propagator.clearStepHandlers();

        // Propagate
        SpacecraftState finalState = propagator.propagate(endDate);

        // we should stop long before endDate
        Assertions.assertTrue(endDate.durationFrom(finalState.getDate()) > 40000.0);
    }

    @Test
    void testEphemerisGenerationIssue14() throws IOException {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        propagator.getInitialState();

        propagator.setOrbitType(OrbitType.CARTESIAN);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(initDate.shiftedBy(dt));
        final BoundedPropagator ephemeris1 = generator.getGeneratedEphemeris();
        Assertions.assertEquals(initDate, ephemeris1.getMinDate());
        Assertions.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

        propagator.getPVCoordinates(initDate.shiftedBy( 2 * dt), FramesFactory.getEME2000());
        propagator.getPVCoordinates(initDate.shiftedBy(-2 * dt), FramesFactory.getEME2000());

        // the new propagations should not have changed ephemeris1
        Assertions.assertEquals(initDate, ephemeris1.getMinDate());
        Assertions.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

        final BoundedPropagator ephemeris2 = generator.getGeneratedEphemeris();
        Assertions.assertEquals(initDate.shiftedBy(-2 * dt), ephemeris2.getMinDate());
        Assertions.assertEquals(initDate.shiftedBy( 2 * dt), ephemeris2.getMaxDate());

        // generating ephemeris2 should not have changed ephemeris1
        Assertions.assertEquals(initDate, ephemeris1.getMinDate());
        Assertions.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

    }

    @Test
    void testEphemerisAdditionalState() throws IOException {

        // Propagation of the initial at t + dt
        final double dt = -3200;
        final double rate = 2.0;

        propagator.addAdditionalDataProvider(new AdditionalDataProvider<Double>() {
            public String getName() {
                return "squaredA";
            }
            public Double getAdditionalData(SpacecraftState state) {
                return state.getOrbit().getA() * state.getOrbit().getA();
            }
        });
        propagator.addAdditionalDerivativesProvider(new AdditionalDerivativesProvider() {
            public String getName() {
                return "extra";
            }
            public int getDimension() {
                return 1;
            }
            public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
                return new CombinedDerivatives(new double[] { rate }, null);
            }
        });
        propagator.setInitialState(propagator.getInitialState().addAdditionalData("extra", 1.5));

        propagator.setOrbitType(OrbitType.CARTESIAN);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(initDate.shiftedBy(dt));
        final BoundedPropagator ephemeris1 = generator.getGeneratedEphemeris();
        Assertions.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMinDate());
        Assertions.assertEquals(initDate, ephemeris1.getMaxDate());
        try {
            ephemeris1.propagate(ephemeris1.getMinDate().shiftedBy(-10.0));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException pe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE, pe.getSpecifier());
        }
        try {
            ephemeris1.propagate(ephemeris1.getMaxDate().shiftedBy(+10.0));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException pe) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER, pe.getSpecifier());
        }

        double shift = -60;
        SpacecraftState s = ephemeris1.propagate(initDate.shiftedBy(shift));
        Assertions.assertEquals(2, s.getAdditionalDataValues().size());
        Assertions.assertTrue(s.hasAdditionalData("squaredA"));
        Assertions.assertTrue(s.hasAdditionalData("extra"));
        Assertions.assertEquals(s.getOrbit().getA() * s.getOrbit().getA(), s.getAdditionalState("squaredA")[0], 1.0e-10);
        Assertions.assertEquals(1.5 + shift * rate, s.getAdditionalState("extra")[0], 1.0e-10);

        try {
            ephemeris1.resetInitialState(s);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }

    }

    @Test
    void testIssue157() {
        try {
            Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngleType.MEAN,
                                             FramesFactory.getTOD(false),
                                             new AbsoluteDate(2003, 5, 6, TimeScalesFactory.getUTC()),
                                             Constants.EIGEN5C_EARTH_MU);
            ToleranceProvider.of(CartesianToleranceProvider.of(1)).getTolerances(orbit, OrbitType.KEPLERIAN);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException pe) {
            Assertions.assertEquals(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, pe.getSpecifier());
        }
    }

    @Test
    void testInternalEventDetectorsFromAttitudeProvider() {
        // GIVEN
        propagator.resetInitialState(initialState);
        final AbsoluteDate epoch = propagator.getInitialState().getDate();
        final AbsoluteDate interruptingDate = epoch.shiftedBy(1);
        propagator.setAttitudeProvider(new InterruptingAttitudeProvider(interruptingDate));
        // WHEN
        final SpacecraftState state = propagator.propagate(interruptingDate.shiftedBy(10));
        // THEN
        Assertions.assertEquals(state.getDate(), interruptingDate);
    }

    private static class InterruptingAttitudeProvider extends FrameAlignedProvider {

        private final AbsoluteDate interruptingDate;

        public InterruptingAttitudeProvider(final AbsoluteDate interruptingDate) {
            super(Rotation.IDENTITY);
            this.interruptingDate = interruptingDate;
        }

        @Override
        public Stream<EventDetector> getEventDetectors() {
            final DateDetector detector = new DateDetector(interruptingDate).withHandler(new StopOnEvent());
            return Stream.of(detector);
        }
    }

    private static class CheckingHandler implements EventHandler {

        private final Action actionOnEvent;
        private boolean gotHere;
        private boolean isFinished = false;

        public CheckingHandler(final Action actionOnEvent) {
            this.actionOnEvent = actionOnEvent;
            this.gotHere       = false;
        }

        public void assertEvent(boolean expected) {
            Assertions.assertEquals(expected, gotHere);
        }

        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
            gotHere = true;
            return actionOnEvent;
        }

        @Override
        public void finish(SpacecraftState finalState, EventDetector detector) {
            isFinished = true;
        }
    }

    @Test
    void testParallelismIssue258() throws InterruptedException, ExecutionException, FileNotFoundException {

        Utils.setDataRoot("regular-data:atmosphere:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final double mu = GravityFieldFactory.getNormalizedProvider(2, 2).getMu();

        // Geostationary transfer orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.72831215; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascension of ascending node
        final double lM = 0; // mean anomaly
        final Frame inertialFrame = FramesFactory.getEME2000();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate initialDate = new AbsoluteDate(2003, 1, 1, 00, 00, 00.000, utc);
        final Orbit initialOrbit = new CartesianOrbit( new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngleType.MEAN,
                                                                          inertialFrame, initialDate, mu));
        final SpacecraftState initialState = new SpacecraftState(initialOrbit).withMass( 1000);

        // initialize the testing points
        final List<SpacecraftState> states = new ArrayList<SpacecraftState>();
        final NumericalPropagator propagator = createPropagator(initialState, OrbitType.CARTESIAN, PositionAngleType.TRUE);
        final double samplingStep = 10000.0;
        propagator.setStepHandler(samplingStep, state -> states.add(state));
        propagator.propagate(initialDate.shiftedBy(5 * samplingStep));

        // compute reference errors, using serial computation in a for loop
        final double[][] referenceErrors = new double[states.size() - 1][];
        for (int startIndex = 0; startIndex < states.size() - 1; ++startIndex) {
            referenceErrors[startIndex] = recomputeFollowing(startIndex, states);
        }

        final Consumer<SpacecraftState> checker = point -> {
            try {
                final int startIndex = states.indexOf(point);
                double[] errors = recomputeFollowing(startIndex, states);
                for (int k = 0; k < errors.length; ++k) {
                    Assertions.assertEquals(
                                        referenceErrors[startIndex][k], errors[k],
                                        1.0e-9,startIndex + " to " + (startIndex + k + 1));
                }
            } catch (OrekitException oe) {
                Assertions.fail(oe.getLocalizedMessage());
            }
        };

        // serial propagation using Stream
        states.stream().forEach(checker);

        // parallel propagation using parallelStream
        states.parallelStream().forEach(checker);

    }

    @Test
    void testShiftKeplerianEllipticTrueWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngleType.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftKeplerianEllipticTrueWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftKeplerianEllipticEccentricWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngleType.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftKeplerianEllipticEcentricWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngleType.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftKeplerianEllipticMeanWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngleType.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftKeplerianEllipticMeanWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngleType.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftKeplerianHyperbolicTrueWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngleType.TRUE, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    void testShiftKeplerianHyperbolicTrueWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngleType.TRUE, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    void testShiftKeplerianHyperbolicEccentricWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngleType.ECCENTRIC, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    void testShiftKeplerianHyperbolicEcentricWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngleType.ECCENTRIC, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    void testShiftKeplerianHyperbolicMeanWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngleType.MEAN, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    void testShiftKeplerianHyperbolicMeanWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngleType.MEAN, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    void testShiftCartesianEllipticTrueWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngleType.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftCartesianEllipticTrueWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngleType.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftCartesianEllipticEccentricWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngleType.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftCartesianEllipticEcentricWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngleType.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftCartesianEllipticMeanWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngleType.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftCartesianEllipticMeanWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngleType.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftCartesianHyperbolicTrueWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngleType.TRUE, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    void testShiftCartesianHyperbolicTrueWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngleType.TRUE, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    void testShiftCartesianHyperbolicEccentricWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngleType.ECCENTRIC, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    void testShiftCartesianHyperbolicEcentricWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngleType.ECCENTRIC, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    void testShiftCartesianHyperbolicMeanWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngleType.MEAN, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    void testShiftCartesianHyperbolicMeanWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngleType.MEAN, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    void testShiftCircularTrueWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngleType.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftCircularTrueWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngleType.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftCircularEccentricWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngleType.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftCircularEcentricWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngleType.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftCircularMeanWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngleType.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftCircularMeanWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngleType.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftEquinoctialTrueWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftEquinoctialTrueWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngleType.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftEquinoctialEccentricWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngleType.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftEquinoctialEcentricWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngleType.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    void testShiftEquinoctialMeanWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngleType.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    void testShiftEquinoctialMeanWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngleType.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    private static void doTestShift(final CartesianOrbit orbit, final OrbitType orbitType,
                                    final PositionAngleType angleType, final boolean withDerivatives,
                                    final double error60s, final double error120s,
                                    final double error300s, final double error600s,
                                    final double error900s)
        {

        Utils.setDataRoot("regular-data:atmosphere:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final NumericalPropagator np = createPropagator(new SpacecraftState(orbit), orbitType, angleType);

        // the reference date for shifts is set at 60s, so the propagator can provide derivatives if needed
        // (derivatives are not available in the initial orbit)
        final AbsoluteDate reference = orbit.getDate().shiftedBy(60.0);
        final ShiftChecker checker   = new ShiftChecker(withDerivatives, orbitType, angleType,
                                                        error60s,
                                                        error120s, error300s,
                                                        error600s, error900s);
        np.addEventDetector(new DateDetector(reference,
                                             reference.shiftedBy( 60.0),
                                             reference.shiftedBy(120.0),
                                             reference.shiftedBy(300.0),
                                             reference.shiftedBy(600.0),
                                             reference.shiftedBy(900.0)).
                            withMaxCheck(30.0).
                            withThreshold(1.0e-9).
                            withHandler(checker));
        np.propagate(reference.shiftedBy(1000.0));
    }

    private static class ShiftChecker implements EventHandler {

        private final boolean       withDerivatives;
        private final OrbitType     orbitType;
        private final PositionAngleType angleType;
        private final double        error60s;
        private final double        error120s;
        private final double        error300s;
        private final double        error600s;
        private final double        error900s;
        private SpacecraftState     referenceState;

        ShiftChecker(final boolean withDerivatives, final OrbitType orbitType,
                     final PositionAngleType angleType, final double error60s,
                     final double error120s, final double error300s,
                     final double error600s, final double error900s) {
            this.withDerivatives = withDerivatives;
            this.orbitType       = orbitType;
            this.angleType       = angleType;
            this.error60s        = error60s;
            this.error120s       = error120s;
            this.error300s       = error300s;
            this.error600s       = error600s;
            this.error900s       = error900s;
            this.referenceState  = null;
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector,
                                    final boolean increasing) {
            if (referenceState == null) {
                // first event, we retrieve the reference state for later use
                if (withDerivatives) {
                    referenceState = s;
                } else {
                    // remove derivatives, to check accuracy of the shiftedBy method decreases without them
                    final double[] stateVector = new double[6];
                    final Orbit o = s.getOrbit();
                    orbitType.mapOrbitToArray(o, angleType, stateVector, null);
                    final Orbit fixedOrbit = orbitType.mapArrayToOrbit(stateVector, null, angleType,
                                                                       o.getDate(), o.getMu(), o.getFrame());
                    referenceState = new SpacecraftState(fixedOrbit, s.getAttitude()).withMass(s.getMass());
                }
            } else {
                // recurring event, we compare with the shifted reference state
                final double dt = s.getDate().durationFrom(referenceState.getDate());
                final SpacecraftState shifted = referenceState.shiftedBy(dt);
                final double error = Vector3D.distance(shifted.getPosition(),
                                                       s.getPosition());
                switch ((int) FastMath.rint(dt)) {
                    case 60 :
                        Assertions.assertEquals(error60s,  error, 0.01 * error60s);
                        break;
                    case 120 :
                        Assertions.assertEquals(error120s, error, 0.01 * error120s);
                        break;
                    case 300 :
                        Assertions.assertEquals(error300s, error, 0.01 * error300s);
                        break;
                    case 600 :
                        Assertions.assertEquals(error600s, error, 0.01 * error600s);
                        break;
                    case 900 :
                        Assertions.assertEquals(error900s, error, 0.01 * error900s);
                        break;
                    default :
                        // this should never happen
                        Assertions.fail("no error set for dt = " + dt);
                        break;
                }
            }
            return Action.CONTINUE;
        }

    }

    /** Test de-activation of event detection and step handling.
     *  When propagating out of start and target date in propagate(startDate, targetDate)
     *  <p>See issue 449 in Orekit forge and
     *  {@link org.orekit.propagation.Propagator#propagate(AbsoluteDate, AbsoluteDate)}.
     *  </p>
     */
    @Test
    void testEventAndStepHandlerDeactivationIssue449() {

        // Setup
        RecordAndContinue recordAndContinue = new RecordAndContinue();
        DateDetector dateDetector = new DateDetector(initDate.shiftedBy(10.),
                                                     initDate.shiftedBy(15.),
                                                     initDate.shiftedBy(20.)).
                                    withMaxCheck(1).
                                    withThreshold(1e-1).
                                    withHandler(recordAndContinue);

        propagator.addEventDetector(dateDetector);

        final AbsoluteDate startDate = initDate.shiftedBy(30.);
        final AbsoluteDate finalDate = initDate.shiftedBy(40.);

        final DateRecorderHandler dateRecorderHandler = new DateRecorderHandler(startDate, finalDate);
        propagator.setStepHandler(1.0, dateRecorderHandler);

        // Action
        propagator.propagate(startDate, finalDate);

        // Verify
        // No event is detected
        Assertions.assertEquals(0, recordAndContinue.getEvents().size());

        // Handler is deactivated (no dates recorded between start and stop date)
        Assertions.assertEquals(0, dateRecorderHandler.handledDatesOutOfInterval.size());
    }

    @Test
    void testResetStateForward() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                   new TimeComponents(14, 0, 0),
                                                   TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                                           eme2000,
                                           date, Constants.EIGEN5C_EARTH_MU);
        final NumericalPropagator propagator =
                        new NumericalPropagator(new LutherIntegrator(300.0),
                                                new LofOffset(eme2000, LOFType.LVLH));
        propagator.resetInitialState(new SpacecraftState(orbit));

        // maneuver along Z in attitude aligned with LVLH will change orbital plane
        final AbsoluteDate maneuverDate = date.shiftedBy(1000.0);
        propagator.addEventDetector(new ImpulseManeuver(new DateDetector(maneuverDate),
                                                        new Vector3D(0.0, 0.0, -100.0),
                                                        350.0));

        final Vector3D initialNormal = orbit.getPVCoordinates().getMomentum();
        propagator.setStepHandler(60.0, state -> {
            final Vector3D currentNormal = state.getPVCoordinates().getMomentum();
            if (state.getDate().isBefore(maneuverDate)) {
                Assertions.assertEquals(0.000, Vector3D.angle(initialNormal, currentNormal), 1.0e-3);
            } else {
                Assertions.assertEquals(0.014, Vector3D.angle(initialNormal, currentNormal), 1.0e-3);
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(1500.0));

    }

    @Test
    void testResetStateBackward() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                   new TimeComponents(14, 0, 0),
                                                   TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                                           eme2000,
                                           date, Constants.EIGEN5C_EARTH_MU);
        final NumericalPropagator propagator =
                        new NumericalPropagator(new LutherIntegrator(300.0),
                                                new LofOffset(eme2000, LOFType.LVLH));
        propagator.resetInitialState(new SpacecraftState(orbit));

        // maneuver along Z in attitude aligned with LVLH will change orbital plane
        final AbsoluteDate maneuverDate = date.shiftedBy(-1000.0);
        propagator.addEventDetector(new ImpulseManeuver(new DateDetector(maneuverDate),
                                                        new Vector3D(0.0, 0.0, -100.0),
                                                        350.0));

        final Vector3D initialNormal = orbit.getPVCoordinates().getMomentum();
        propagator.setStepHandler(60.0, state -> {
            final Vector3D currentNormal = state.getPVCoordinates().getMomentum();
            if (state.getDate().isAfter(maneuverDate)) {
                Assertions.assertEquals(0.000, Vector3D.angle(initialNormal, currentNormal), 1.0e-3);
            } else {
                Assertions.assertEquals(0.014, Vector3D.angle(initialNormal, currentNormal), 1.0e-3);
            }
        });

        propagator.propagate(orbit.getDate().shiftedBy(-1500.0));

    }

    @Test
    void testAdditionalDerivatives() {

        final Frame eme2000 = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                   new TimeComponents(14, 0, 0),
                                                   TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05, PositionAngleType.MEAN,
                                           eme2000,
                                           date, Constants.EIGEN5C_EARTH_MU);
        NumericalPropagatorBuilder builder = new NumericalPropagatorBuilder(initialOrbit,
                new DormandPrince853IntegratorBuilder(0.02, 0.2, 1.), PositionAngleType.TRUE, 10);
        NumericalPropagator propagator = (NumericalPropagator) builder.buildPropagator();

        IntStream.
        range(0, 2).
        mapToObj(i -> new EmptyDerivativeProvider("test_provider_" + i, new double[] { 10 * i, 20 * i })).
        forEach(provider -> addDerivativeProvider(propagator, provider));

        EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(initialOrbit.getDate().shiftedBy(600));
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();
        final SpacecraftState finalState = ephemeris.propagate(initialOrbit.getDate().shiftedBy(300));
        Assertions.assertEquals(2,    finalState.getAdditionalDataValues().size());
        Assertions.assertEquals(2,    finalState.getAdditionalState("test_provider_0").length);
        Assertions.assertEquals(0.0,  finalState.getAdditionalState("test_provider_0")[0], 1.0e-15);
        Assertions.assertEquals(0.0,  finalState.getAdditionalState("test_provider_0")[1], 1.0e-15);
        Assertions.assertEquals(2,    finalState.getAdditionalState("test_provider_1").length);
        Assertions.assertEquals(10.0, finalState.getAdditionalState("test_provider_1")[0], 1.0e-15);
        Assertions.assertEquals(20.0, finalState.getAdditionalState("test_provider_1")[1], 1.0e-15);
    }

    private void addDerivativeProvider(NumericalPropagator propagator, EmptyDerivativeProvider provider) {
        SpacecraftState initialState = propagator.getInitialState();
        propagator.setInitialState(initialState.addAdditionalData(provider.getName(), provider.getInitialState()));
        propagator.addAdditionalDerivativesProvider(provider);
    }

    private static class EmptyDerivativeProvider implements AdditionalDerivativesProvider {

        private final String name;
        private final double[] state;

        public EmptyDerivativeProvider(String name, double[] state) {
            this.name = name;
            this.state = state;
        }

        @Override
        public CombinedDerivatives combinedDerivatives(SpacecraftState s) {
            return new CombinedDerivatives(new double[getDimension()], null);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getDimension() {
            return state.length;
        }

        public double[] getInitialState() {
            return state;
        }
    }

    @Test
    void testInfinitePropagation() {

        Utils.setDataRoot("regular-data:atmosphere:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        final NumericalPropagator propag = createPropagator(initialState, OrbitType.KEPLERIAN, PositionAngleType.TRUE);

        // Stop condition
        final double convergenceThreshold = 1e-9;
        propag.addEventDetector(new DateDetector(initialState.getDate().shiftedBy(60)).
                                withMaxCheck(1e10).
                                withThreshold(convergenceThreshold));

        // Propagate until the stop condition is reached
        final SpacecraftState finalState =  propag.propagate(AbsoluteDate.FUTURE_INFINITY);

        // Check that the expected final state was reached
        Assertions.assertEquals(60, finalState.getDate().durationFrom(initialState.getDate()), convergenceThreshold);

    }

    @Test
    void getIntegratorNameTest() {
        // GIVEN
        final String expectedName = "Name";
        final ODEIntegrator mockedIntegrator = Mockito.mock(ODEIntegrator.class);
        Mockito.when(mockedIntegrator.getName()).thenReturn(expectedName);
        // WHEN
        final NumericalPropagator numericalPropagator = new NumericalPropagator(mockedIntegrator);
        final String actualName = numericalPropagator.getIntegratorName();
        // THEN
        Assertions.assertEquals(expectedName, actualName);
    }

    @Test
    void testIssue1395() {
        // GIVEN
        final Orbit initialOrbit = createEllipticOrbit();
        final ClassicalRungeKuttaIntegrator rungeKuttaIntegrator = new ClassicalRungeKuttaIntegrator(10);
        final NumericalPropagator numericalPropagator = new NumericalPropagator(rungeKuttaIntegrator);
        final SpacecraftState state = new SpacecraftState(initialOrbit);
        final String name = "test";
        numericalPropagator.setInitialState(state.addAdditionalData(name, 0.));
        numericalPropagator.addAdditionalDerivativesProvider(mockDerivativeProvider(name));
        numericalPropagator.addForceModel(createForceModelBasedOnAdditionalState(name));
        // WHEN & THEN
        final AbsoluteDate epoch = initialOrbit.getDate();
        final SpacecraftState propagateState = Assertions.assertDoesNotThrow(() ->
                numericalPropagator.propagate(epoch.shiftedBy(10.)));
        Assertions.assertNotEquals(epoch, propagateState.getDate());
    }

    private static AdditionalDerivativesProvider mockDerivativeProvider(final String name) {
        final AdditionalDerivativesProvider mockedProvider = Mockito.mock(AdditionalDerivativesProvider.class);
        final int dimension = 1;
        Mockito.when(mockedProvider.getDimension()).thenReturn(dimension);
        Mockito.when(mockedProvider.getName()).thenReturn(name);
        final double[] yDot = new double[dimension];
        final CombinedDerivatives combinedDerivatives = new CombinedDerivatives(yDot, null);
        Mockito.when(mockedProvider.combinedDerivatives(Mockito.any(SpacecraftState.class)))
                .thenReturn(combinedDerivatives);
        return mockedProvider;
    }

    private static ForceModel createForceModelBasedOnAdditionalState(final String name) {
        return new ForceModel() {

            @Override
            public void init(SpacecraftState initialState, AbsoluteDate target) {
                ForceModel.super.init(initialState, target);
                initialState.getAdditionalState(name);
            }

            @Override
            public boolean dependsOnPositionOnly() {
                return false;
            }

            @Override
            public Vector3D acceleration(SpacecraftState s, double[] parameters) {
                return Vector3D.ZERO;
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
                return null; // not used
            }

            @Override
            public List<ParameterDriver> getParametersDrivers() {
                return new ArrayList<>();
            }
        };
    }

    /** Record the dates treated by the handler.
     *  If they are out of an interval defined by a start and final date.
     */
    private static class DateRecorderHandler implements OrekitFixedStepHandler {

        /** Start date of the propagation. */
        private final AbsoluteDate startDate;

        /** Final date of the propagation. */
        private final AbsoluteDate finalDate;

        /** List of handled date. Recorded only if they are out of the propagation interval. */
        public final List<AbsoluteDate> handledDatesOutOfInterval;

        DateRecorderHandler(final AbsoluteDate startDate, final AbsoluteDate finalDate) {
          this.startDate = startDate;
          this.finalDate = finalDate;
          this.handledDatesOutOfInterval = new ArrayList<>();
        }

        @Override
        public void handleStep(SpacecraftState currentState)
                {
          final AbsoluteDate date = currentState.getDate();
          if (date.compareTo(startDate) < 0 || date.compareTo(finalDate) > 0) {
            handledDatesOutOfInterval.add(currentState.getDate());
          }
        }
      }

    /**
     * Assume we have 5 epochs, we will propagate from the input epoch to all the following epochs.
     *   If we have [0, 1, 2, 3, 4], and input is 2, then we will do 2->3, 2->4.
     * @param startIndex index of start state
     * @return position error for recomputed following points
     */
    private static double[] recomputeFollowing(final int startIndex, List<SpacecraftState> allPoints)
        {
        SpacecraftState startState = allPoints.get(startIndex);
        NumericalPropagator innerPropagator = createPropagator(startState, OrbitType.CARTESIAN, PositionAngleType.TRUE);
        double[] errors = new double[allPoints.size() - startIndex - 1];
        for (int endIndex = startIndex + 1; endIndex < allPoints.size(); ++endIndex) {
            final TimeStampedPVCoordinates reference  = allPoints.get(endIndex).getPVCoordinates();
            final TimeStampedPVCoordinates recomputed = innerPropagator.propagate(reference.getDate()).getPVCoordinates();
            errors[endIndex - startIndex - 1] = Vector3D.distance(recomputed.getPosition(), reference.getPosition());
        }
        return errors;
    }

    private synchronized static NumericalPropagator createPropagator(SpacecraftState spacecraftState,
                                                                     OrbitType orbitType, PositionAngleType angleType)
        {

        final double minStep                         = 0.001;
        final double maxStep                         = 120.0;
        final double positionTolerance               = 0.1;
        final int    degree                          = 20;
        final int    order                           = 20;
        final double spacecraftArea                  = 1.0;
        final double spacecraftDragCoefficient       = 2.0;
        final double spacecraftReflectionCoefficient = 2.0;

        // propagator main configuration
        final double[][] tol           = ToleranceProvider.of(CartesianToleranceProvider.of(positionTolerance))
                .getTolerances(spacecraftState.getOrbit(), orbitType);
        final ODEIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        final NumericalPropagator np   = new NumericalPropagator(integrator);
        np.setOrbitType(orbitType);
        np.setPositionAngleType(angleType);
        np.setInitialState(spacecraftState);

        // Earth gravity field
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final NormalizedSphericalHarmonicsProvider harmonicsGravityProvider = GravityFieldFactory.getNormalizedProvider(degree, order);
        np.addForceModel(new HolmesFeatherstoneAttractionModel(earth.getBodyFrame(), harmonicsGravityProvider));

        // Sun and Moon attraction
        np.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        np.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));

        // atmospheric drag
        MarshallSolarActivityFutureEstimation msafe =
                        new MarshallSolarActivityFutureEstimation("Jan2000F10-edited-data\\.txt",
                                                                  MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        DTM2000 atmosphere = new DTM2000(msafe, CelestialBodyFactory.getSun(), earth);
        np.addForceModel(new DragForce(atmosphere, new IsotropicDrag(spacecraftArea, spacecraftDragCoefficient)));

        // solar radiation pressure
        np.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(), earth,
                                                    new IsotropicRadiationSingleCoefficient(spacecraftArea, spacecraftReflectionCoefficient)));

        return np;

    }

    private CartesianOrbit createEllipticOrbit() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(6896874.444705,  1956581.072644,  -147476.245054);
        final Vector3D     velocity     = new Vector3D(169.816407662, -1126.783301861, -7332.745712770);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        return new CartesianOrbit(pv, frame, mu);
    }

    private CartesianOrbit createHyperbolicOrbit() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        return new CartesianOrbit(pv, frame, mu);
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 7})
    void testClearMatricesComputation(final int stateDimension) {
        // GIVEN
        propagator.setupMatricesComputation("stm", MatrixUtils.createRealIdentityMatrix(stateDimension), null);
        propagator.setInitialState(initialState);
        propagator.setResetAtEnd(false);
        final Maneuver maneuver = new ConstantThrustManeuver(initDate, 10, 1., 100, Vector3D.MINUS_I);
        propagator.addForceModel(maneuver);
        maneuver.getParameterDriver(ParameterDrivenDateIntervalDetector.MEDIAN_SUFFIX).setSelected(true);
        maneuver.getParameterDriver(ParameterDrivenDateIntervalDetector.DURATION_SUFFIX).setSelected(true);
        final AbsoluteDate targetDate = initialState.getDate().shiftedBy(1);
        propagator.propagate(targetDate);
        // WHEN
        propagator.clearMatricesComputation();
        // THEN
        Assertions.assertTrue(propagator.getAdditionalDataProviders().isEmpty());
        Assertions.assertTrue(propagator.getAdditionalDerivativesProviders().isEmpty());
    }

    @Test
    void testFinalAttitudeWithForcesNeedingRates() {
        final ForceModel dummyForceDependingOnRates = new ForceModel() {
            @Override
            public boolean dependsOnAttitudeRate() {
                return true;
            }

            @Override
            public boolean dependsOnPositionOnly() {
                return false;
            }

            @Override
            public Vector3D acceleration(SpacecraftState s, double[] parameters) {
                return Vector3D.ZERO;
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
                return null;
            }

            @Override
            public List<ParameterDriver> getParametersDrivers() {
                return Collections.emptyList();
            }
        };
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(dummyForceDependingOnRates);
        testTemplateFinalAttitudeWithoutForcesNeedingRates(forceModels);
    }

    @Test
    void testFinalAttitudeWithoutForcesNeedingRates() {
        testTemplateFinalAttitudeWithoutForcesNeedingRates(new ArrayList<>());
    }

    private void testTemplateFinalAttitudeWithoutForcesNeedingRates(final List<ForceModel> forceModels) {
        // GIVEN
        final AttitudeProvider attitudeProvider = createAttitudeProviderWithNonZeroRates();
        propagator.setAttitudeProvider(attitudeProvider);
        for (final ForceModel forceModel : forceModels) {
            propagator.addForceModel(forceModel);
        }
        // WHEN
        final SpacecraftState state = propagator.propagate(propagator.getInitialState().getDate().shiftedBy(10.));
        // THEN
        final Attitude attitude = state.getAttitude();
        Assertions.assertNotEquals(Vector3D.ZERO, attitude.getSpin());
        Assertions.assertNotEquals(Vector3D.ZERO, attitude.getRotationAcceleration());
    }

    private AttitudeProvider createAttitudeProviderWithNonZeroRates() {
        return new AttitudeProvider() {
            @Override
            public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
                return createAttitudeWithNonZeroRates(date, frame);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv, FieldAbsoluteDate<T> date, Frame frame) {
                return null;
            }
        };
    }

    private Attitude createAttitudeWithNonZeroRates(final AbsoluteDate date, final Frame frame) {
        final AngularCoordinates angularCoordinates = new AngularCoordinates(Rotation.IDENTITY,
                Vector3D.PLUS_K, Vector3D.MINUS_I);
        return new Attitude(date, frame, angularCoordinates);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        mu  = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        double[][] tolerance = ToleranceProvider.of(CartesianToleranceProvider.of(0.001)).getTolerances(orbit,
                OrbitType.EQUINOCTIAL, PositionAngleType.TRUE);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
    }

    @AfterEach
    public void tearDown() {
        initDate = null;
        initialState = null;
        propagator = null;
    }

    /**
     * Adapter class for {@link ForceModel} so that sub classes only have to implement the
     * methods they want.
     */
    private static class ForceModelAdapter implements ForceModel {

        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        @Override
        public boolean isSupported(String name) {
            return false;
        }

        @Override
        public void addContribution(SpacecraftState s, TimeDerivativesEquations adder) {
        }

        @Override
        public <T extends CalculusFieldElement<T>> void
        addContribution(FieldSpacecraftState<T> s,
                        FieldTimeDerivativesEquations<T> adder) {
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
            {
            return Vector3D.ZERO;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters)
            {
            return FieldVector3D.getZero(s.getDate().getField());
        }

        @Override
        public Stream<EventDetector> getEventDetectors() {
            return Stream.empty();
        }

        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
            return Stream.empty();
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }

}

