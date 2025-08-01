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
package org.orekit.forces.maneuvers;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.*;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnIncreasing;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

class ImpulseManeuverTest {

    @ParameterizedTest
    @EnumSource(Action.class)
    void testInclinationManeuver(final Action action) {
        final Orbit initialOrbit =
            new KeplerianOrbit(24532000.0, 0.72, 0.3, FastMath.PI, 0.4, 2.0 + 4 * FastMath.PI,
                               PositionAngleType.MEAN, FramesFactory.getEME2000(),
                               new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                new TimeComponents(14, 18, 37),
                                                TimeScalesFactory.getUTC()),
                               3.986004415e14);
        final double a  = initialOrbit.getA();
        final double e  = initialOrbit.getE();
        final double i  = initialOrbit.getI();
        final double mu = initialOrbit.getMu();
        final double vApo = FastMath.sqrt(mu * (1 - e) / (a * (1 + e)));
        double dv = 0.99 * FastMath.tan(i) * vApo;
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit,
                                                                 new LofOffset(initialOrbit.getFrame(), LOFType.LVLH_CCSDS));
        final NodeDetector trigger = new NodeDetector(initialOrbit, FramesFactory.getEME2000());
        propagator.addEventDetector(new ImpulseManeuver(trigger.withHandler((s, detector, increasing) -> action),
                                                        new Vector3D(dv, Vector3D.PLUS_J), 400.0));
        SpacecraftState propagated = propagator.propagate(initialOrbit.getDate().shiftedBy(8000));
        Assertions.assertEquals(0.0028257, propagated.getOrbit().getI(), 1.0e-6);
        Assertions.assertEquals(0.442476 + 6 * FastMath.PI, propagated.getOrbit().getLv(), 1.0e-6);
    }

    @Test
    void testInertialManeuver() {
        final double mu = CelestialBodyFactory.getEarth().getGM();

        final double initialX = 7100e3;
        final double initialY = 0.0;
        final double initialZ = 1300e3;
        final double initialVx = 0;
        final double initialVy = 8000;
        final double initialVz = 1000;

        final Vector3D position = new Vector3D(initialX, initialY, initialZ);
        final Vector3D velocity = new Vector3D(initialVx, initialVy, initialVz);
        final AbsoluteDate epoch = new AbsoluteDate(2010, 1, 1, 0, 0, 0, TimeScalesFactory.getUTC());
        final TimeStampedPVCoordinates state = new TimeStampedPVCoordinates(epoch, position, velocity, Vector3D.ZERO);
        final Orbit initialOrbit = new CartesianOrbit(state, FramesFactory.getEME2000(), mu);

        final double totalPropagationTime = 0.00001;
        final double driftTimeInSec = totalPropagationTime / 2.0;
        final double deltaX = 0.01;
        final double deltaY = 0.02;
        final double deltaZ = 0.03;
        final double isp = 300;

        final Vector3D deltaV = new Vector3D(deltaX, deltaY, deltaZ);

        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, new LofOffset(initialOrbit.getFrame(), LOFType.VNC));
        DateDetector dateDetector = new DateDetector(epoch.shiftedBy(driftTimeInSec));
        FrameAlignedProvider attitudeOverride = new FrameAlignedProvider(new Rotation(RotationOrder.XYX,
                                                                              RotationConvention.VECTOR_OPERATOR,
                                                                              0, 0, 0));
        ImpulseManeuver burnAtEpoch = new ImpulseManeuver(dateDetector.withThreshold(driftTimeInSec/4), attitudeOverride, deltaV, isp);
        Assertions.assertEquals(isp, burnAtEpoch.getIsp(), 1.0e-15);
        Assertions.assertSame(dateDetector.getClass(), burnAtEpoch.getTrigger().getClass());
        propagator.addEventDetector(burnAtEpoch);

        SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(totalPropagationTime));

        final double finalVxExpected = initialVx + deltaX;
        final double finalVyExpected = initialVy + deltaY;
        final double finalVzExpected = initialVz + deltaZ;
        final double maneuverTolerance = 1e-4;

        final Vector3D finalVelocity = finalState.getVelocity();
        Assertions.assertEquals(finalVxExpected, finalVelocity.getX(), maneuverTolerance);
        Assertions.assertEquals(finalVyExpected, finalVelocity.getY(), maneuverTolerance);
        Assertions.assertEquals(finalVzExpected, finalVelocity.getZ(), maneuverTolerance);

    }

    @Test
    void testEventOccurredEventSlopeFilter() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final ApsideDetector detector = new ApsideDetector(orbit);
        final ImpulseManeuver maneuver = new ImpulseManeuver(new EventSlopeFilter<>(detector,
                FilterType.TRIGGER_ONLY_INCREASING_EVENTS), Vector3D.ZERO, Double.POSITIVE_INFINITY);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        // WHEN & THEN
        propagator.addEventDetector(maneuver);
        Assertions.assertDoesNotThrow(() -> propagator.propagate(orbit.getDate().shiftedBy(1e5)));
    }

    @Test
    void testWithDetectionSettings() {
        // GIVEN
        final DateDetector dateDetector = new DateDetector();
        final ImpulseManeuver maneuver = new ImpulseManeuver(dateDetector, Vector3D.ZERO, 1.);
        final EventDetectionSettings expectedSettings = new EventDetectionSettings(1., 2., 3);
        // WHEN
        final ImpulseManeuver maneuverWithSettings = maneuver.withDetectionSettings(expectedSettings);
        // THEN
        Assertions.assertEquals(expectedSettings.getThreshold(), maneuverWithSettings.getThreshold());
        Assertions.assertEquals(expectedSettings.getMaxIterationCount(), maneuverWithSettings.getMaxIterationCount());
        Assertions.assertEquals(expectedSettings.getMaxCheckInterval(), maneuverWithSettings.getMaxCheckInterval());
    }

    @Test
    void testResetStateAttitudeOverride() {
        // GIVEN
        final AbsolutePVCoordinates pvCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new Vector3D(1., 2, 3), new Vector3D(4, 5, 6));
        final ImpulseManeuver impulseManeuver = new ImpulseManeuver(new DateDetector(),
                new FrameAlignedProvider(pvCoordinates.getFrame()), Vector3D.ZERO, 1.);
        final SpacecraftState expectedSTate = new SpacecraftState(pvCoordinates);
        // WHEN
        final SpacecraftState actualSTate = impulseManeuver.getHandler().resetState(impulseManeuver, expectedSTate);
        // THEN
        compareStates(expectedSTate, actualSTate);
    }

    @Test
    void testResetState() {
        // GIVEN
        final AbsolutePVCoordinates pvCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new Vector3D(1., 2, 3), new Vector3D(4, 5, 6));
        final ImpulseManeuver impulseManeuver = new ImpulseManeuver(new DateDetector(), Vector3D.ZERO, 1.);
        final SpacecraftState expectedSTate = new SpacecraftState(pvCoordinates);
        // WHEN
        final SpacecraftState actualSTate = impulseManeuver.getHandler().resetState(impulseManeuver, expectedSTate);
        // THEN
        compareStates(expectedSTate, actualSTate);
    }

    private static void compareStates(final SpacecraftState expectedState, final SpacecraftState actualState) {
        Assertions.assertEquals(expectedState.getDate(), actualState.getDate());
        Assertions.assertEquals(expectedState.getMass(), actualState.getMass());
        Assertions.assertEquals(expectedState.getAttitude(), actualState.getAttitude());
        Assertions.assertEquals(expectedState.getPosition(), actualState.getPosition());
        Assertions.assertEquals(expectedState.getVelocity(),
                actualState.getVelocity());
    }

    @Test
    void testBackward() {

        final AbsoluteDate iniDate = new AbsoluteDate(2003, 5, 1, 17, 30, 0.0, TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(7e6, 1.0e-4, FastMath.toRadians(98.5),
                                          FastMath.toRadians(87.0), FastMath.toRadians(216.1807),
                                          FastMath.toRadians(319.779), PositionAngleType.MEAN,
                                          FramesFactory.getEME2000(), iniDate,
                                          Constants.EIGEN5C_EARTH_MU);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit,
                                                                 new LofOffset(initialOrbit.getFrame(),
                                                                               LOFType.VNC));
        DateDetector dateDetector = new DateDetector(iniDate.shiftedBy(-300));
        Vector3D deltaV = new Vector3D(12.0, 1.0, -4.0);
        final double isp = 300;
        ImpulseManeuver maneuver =
                        new ImpulseManeuver(dateDetector.withMaxCheck(3600.0).
                                withThreshold(1.0e-6), deltaV, isp);
        propagator.addEventDetector(maneuver);

        SpacecraftState finalState = propagator.propagate(initialOrbit.getDate().shiftedBy(-900));

        Assertions.assertTrue(finalState.getMass() > propagator.getInitialState().getMass());
        Assertions.assertTrue(finalState.getDate().compareTo(propagator.getInitialState().getDate()) < 0);

    }

    @Test
    void testBackAndForth() {

        final AttitudeProvider lof = new LofOffset(FramesFactory.getEME2000(), LOFType.VNC);
        final double mu = Constants.EIGEN5C_EARTH_MU;
        final AbsoluteDate iniDate = new AbsoluteDate(2003, 5, 1, 17, 30, 0.0, TimeScalesFactory.getUTC());
        final Orbit pastOrbit = new KeplerianOrbit(7e6, 1.0e-4, FastMath.toRadians(98.5),
                                                   FastMath.toRadians(87.0), FastMath.toRadians(216.1807),
                                                   FastMath.toRadians(319.779), PositionAngleType.MEAN,
                                                   FramesFactory.getEME2000(), iniDate, mu);
        final double pastMass = 2500.0;
        DateDetector dateDetector = new DateDetector(iniDate.shiftedBy(600));
        Vector3D deltaV = new Vector3D(12.0, 1.0, -4.0);
        final double isp = 300;
        ImpulseManeuver maneuver =
                        new ImpulseManeuver(dateDetector.withMaxCheck(3600.0).
                                withThreshold(1.0e-6),
                                            new FrameAlignedProvider(Rotation.IDENTITY),
                                            deltaV, isp);

        double span = 900.0;
        KeplerianPropagator forwardPropagator = new KeplerianPropagator(pastOrbit, lof, mu, pastMass);
        forwardPropagator.addEventDetector(maneuver);
        SpacecraftState futureState = forwardPropagator.propagate(pastOrbit.getDate().shiftedBy(span));

        KeplerianPropagator backwardPropagator = new KeplerianPropagator(futureState.getOrbit(), lof,
                                                                         mu, futureState.getMass());
        backwardPropagator.addEventDetector(maneuver);
        SpacecraftState rebuiltPast = backwardPropagator.propagate(pastOrbit.getDate());
        Assertions.assertEquals(0.0,
                            Vector3D.distance(pastOrbit.getPosition(),
                                              rebuiltPast.getPosition()),
                            2.0e-8);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(pastOrbit.getVelocity(),
                                              rebuiltPast.getVelocity()),
                            2.0e-11);
        Assertions.assertEquals(pastMass, rebuiltPast.getMass(), 5.0e-13);

    }

    @Test
    void testAdditionalStateKeplerian() {
        final double mu = CelestialBodyFactory.getEarth().getGM();

        final double initialX = 7100e3;
        final double initialY = 0.0;
        final double initialZ = 1300e3;
        final double initialVx = 0;
        final double initialVy = 8000;
        final double initialVz = 1000;

        final Vector3D position = new Vector3D(initialX, initialY, initialZ);
        final Vector3D velocity = new Vector3D(initialVx, initialVy, initialVz);
        final AbsoluteDate epoch = new AbsoluteDate(2010, 1, 1, 0, 0, 0, TimeScalesFactory.getUTC());
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(epoch, position, velocity, Vector3D.ZERO);
        final Orbit initialOrbit = new CartesianOrbit(pv, FramesFactory.getEME2000(), mu);

        final double totalPropagationTime = 10;
        final double deltaX = 0.01;
        final double deltaY = 0.02;
        final double deltaZ = 0.03;
        final double isp = 300;

        final Vector3D deltaV = new Vector3D(deltaX, deltaY, deltaZ);

        final AttitudeProvider attitudeProvider = new LofOffset(initialOrbit.getFrame(), LOFType.VNC);
        final Attitude initialAttitude = attitudeProvider.getAttitude(initialOrbit, initialOrbit.getDate(), initialOrbit.getFrame());
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, initialAttitude);
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        propagator.resetInitialState(initialState.addAdditionalData("testOnly", -1.0));
        DateDetector dateDetector = new DateDetector(epoch.shiftedBy(0.5 * totalPropagationTime));
        FrameAlignedProvider attitudeOverride = new FrameAlignedProvider(new Rotation(RotationOrder.XYX,
                                                                              RotationConvention.VECTOR_OPERATOR,
                                                                              0, 0, 0));
        ImpulseManeuver burnAtEpoch = new ImpulseManeuver(dateDetector.withThreshold(1.0e-3), attitudeOverride, deltaV, isp);
        propagator.addEventDetector(burnAtEpoch);

        SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(totalPropagationTime));
        Assertions.assertEquals(1, finalState.getAdditionalDataValues().size());
        Assertions.assertEquals(-1.0, finalState.getAdditionalState("testOnly")[0], 1.0e-15);

    }

    @Test
    void testAdditionalStateNumerical() {
        final double mu = CelestialBodyFactory.getEarth().getGM();

        final double initialX = 7100e3;
        final double initialY = 0.0;
        final double initialZ = 1300e3;
        final double initialVx = 0;
        final double initialVy = 8000;
        final double initialVz = 1000;

        final Vector3D position = new Vector3D(initialX, initialY, initialZ);
        final Vector3D velocity = new Vector3D(initialVx, initialVy, initialVz);
        final AbsoluteDate epoch = new AbsoluteDate(2010, 1, 1, 0, 0, 0, TimeScalesFactory.getUTC());
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(epoch, position, velocity, Vector3D.ZERO);
        final Orbit initialOrbit = new CartesianOrbit(pv, FramesFactory.getEME2000(), mu);

        final double totalPropagationTime = 10.0;
        final double deltaX = 0.01;
        final double deltaY = 0.02;
        final double deltaZ = 0.03;
        final double isp = 300;

        final Vector3D deltaV = new Vector3D(deltaX, deltaY, deltaZ);

        final AttitudeProvider attitudeProvider = new LofOffset(initialOrbit.getFrame(), LOFType.VNC);
        final Attitude initialAttitude = attitudeProvider.getAttitude(initialOrbit, initialOrbit.getDate(), initialOrbit.getFrame());

        double[][] tolerances = ToleranceProvider.getDefaultToleranceProvider(10.).getTolerances(initialOrbit, initialOrbit.getType());
        DormandPrince853Integrator integrator = new DormandPrince853Integrator(1.0e-3, 60, tolerances[0], tolerances[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(initialOrbit.getType());
        MatricesHarvester harvester = propagator.setupMatricesComputation("derivatives", null, null);
        propagator.resetInitialState(new SpacecraftState(initialOrbit, initialAttitude));
        DateDetector dateDetector = new DateDetector(epoch.shiftedBy(0.5 * totalPropagationTime));
        FrameAlignedProvider attitudeOverride = new FrameAlignedProvider(new Rotation(RotationOrder.XYX,
                                                                              RotationConvention.VECTOR_OPERATOR,
                                                                              0, 0, 0));
        ImpulseManeuver burnAtEpoch = new ImpulseManeuver(dateDetector.withThreshold(1.0e-3), attitudeOverride, deltaV, isp);
        propagator.addEventDetector(burnAtEpoch);

        SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(totalPropagationTime));
        Assertions.assertEquals(1, finalState.getAdditionalDataValues().size());
        Assertions.assertEquals(36, finalState.getAdditionalState("derivatives").length);

        RealMatrix stateTransitionMatrix = harvester.getStateTransitionMatrix(finalState);
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                double sIJ = stateTransitionMatrix.getEntry(i, j);
                if (j == i) {
                    // dPi/dPj and dVi/dVj are roughly 1 for small propagation times
                    Assertions.assertEquals(1.0, sIJ, 2.0e-4);
                } else if (j == i + 3) {
                    // dVi/dPi is roughly the propagation time for small propagation times
                    Assertions.assertEquals(totalPropagationTime, sIJ, 4.0e-5 * totalPropagationTime);
                } else {
                    // other derivatives are almost zero for small propagation times
                    Assertions.assertEquals(0, sIJ, 1.0e-4);
                }
            }
        }

    }

    /**
     * The test is inspired by the example given by melvina user in the Orekit forum.
     * https://forum.orekit.org/t/python-error-using-impulsemaneuver-with-positionangledetector/771
     */
    @Test
    void testIssue663() {

        // Initial orbit
        final Orbit initialOrbit =
                        new KeplerianOrbit(24532000.0, 0.72, 0.3, FastMath.PI, 0.4, 2.0,
                                           PositionAngleType.MEAN, FramesFactory.getEME2000(),
                                           new AbsoluteDate(new DateComponents(2008, 6, 23),
                                                            new TimeComponents(14, 18, 37),
                                                            TimeScalesFactory.getUTC()),
                                           3.986004415e14);
        // create maneuver's trigger
        final InitializationDetector trigger = new InitializationDetector();

        // create maneuver
        final AttitudeProvider attitudeOverride = new LofOffset(FramesFactory.getEME2000(), LOFType.TNW);
        final Vector3D         deltaVSat        = Vector3D.PLUS_I;
        final double           isp              = 1500.0;
        final ImpulseManeuver maneuver = new ImpulseManeuver(trigger, attitudeOverride, deltaVSat, isp);

        // add maneuver to propagator
        KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, attitudeOverride);
        propagator.addEventDetector(maneuver);

        // propagation
        Assertions.assertFalse(trigger.initialized);
        propagator.propagate(initialOrbit.getDate().shiftedBy(3600.0));
        Assertions.assertTrue(trigger.initialized);

    }

    @Test
    void testControl3DVectorCostType() {

        // GIVEN
        // Initial orbit
        final double        a             = Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 2000.e3;
        final double        e             = 1e-4;
        final double        i             = 0.5;
        final double        pa            = 0.;
        final double        raan          = 6.;
        final double        anomaly       = 1.;
        final PositionAngleType positionAngleType = PositionAngleType.MEAN;
        final Frame gcrf                  = FramesFactory.getGCRF();
        final AbsoluteDate  date          = AbsoluteDate.ARBITRARY_EPOCH;
        final double        mu            = Constants.EGM96_EARTH_MU;

        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, anomaly, positionAngleType, gcrf, date, mu);

        // Thrust configuration
        final DateDetector     dateDetector       = new DateDetector(date.shiftedBy(1000.));
        final AbsoluteDate     endPropagationDate = dateDetector.getDate().shiftedBy(10000.);
        final AttitudeProvider attitudeProvider = new FrameAlignedProvider(gcrf);
        final Vector3D         deltaV             = new Vector3D(2., -1., 0.5);
        final ImpulseProvider impulseProvider = ImpulseProvider.of(deltaV);
        final double           initialMass        = 1000.;
        final double           isp                = 100.;

        // Building propagators
        final KeplerianPropagator propagatorNone    = new KeplerianPropagator(orbit, attitudeProvider, orbit.getMu(), initialMass);
        final KeplerianPropagator propagatorNorm1   = new KeplerianPropagator(orbit, attitudeProvider, orbit.getMu(), initialMass);
        final KeplerianPropagator propagatorNorm2   = new KeplerianPropagator(orbit, attitudeProvider, orbit.getMu(), initialMass);
        final KeplerianPropagator propagatorNormInf = new KeplerianPropagator(orbit, attitudeProvider, orbit.getMu(), initialMass);

        // Add impulse maneuvers
        propagatorNone.addEventDetector(new ImpulseManeuver(dateDetector, attitudeProvider, impulseProvider, isp,
                Control3DVectorCostType.NONE));
        propagatorNorm1.addEventDetector(new ImpulseManeuver(dateDetector, attitudeProvider, impulseProvider, isp,
                Control3DVectorCostType.ONE_NORM));
        propagatorNorm2.addEventDetector(new ImpulseManeuver(dateDetector, attitudeProvider, impulseProvider, isp,
                Control3DVectorCostType.TWO_NORM));
        propagatorNormInf.addEventDetector(new ImpulseManeuver(dateDetector, attitudeProvider, impulseProvider, isp,
                Control3DVectorCostType.INF_NORM));

        // WHEN
        final double finalMassWithNone    = propagatorNone.propagate(endPropagationDate).getMass();
        final double finalMassWithNorm1   = propagatorNorm1.propagate(endPropagationDate).getMass();
        final double finalMassWithNorm2   = propagatorNorm2.propagate(endPropagationDate).getMass();
        final double finalMassWithNormInf = propagatorNormInf.propagate(endPropagationDate).getMass();

        // THEN
        // Assert that we do not find the same final mass when using different control vector norm
        Assertions.assertNotEquals(finalMassWithNorm1, finalMassWithNorm2);
        Assertions.assertNotEquals(finalMassWithNorm1, finalMassWithNormInf);
        Assertions.assertNotEquals(finalMassWithNormInf, finalMassWithNorm2);

        // Assert that final mass is equal to expected mass
        Assertions.assertEquals(initialMass, finalMassWithNone);
        final double factorExponential = -1. / (isp * Constants.G0_STANDARD_GRAVITY);
        Assertions.assertEquals(initialMass * FastMath.exp(deltaV.getNorm1() * factorExponential), finalMassWithNorm1);
        Assertions.assertEquals(initialMass * FastMath.exp(deltaV.getNorm() * factorExponential), finalMassWithNorm2);
        Assertions.assertEquals(initialMass * FastMath.exp(deltaV.getNormInf() * factorExponential), finalMassWithNormInf);
    }

    @Deprecated
    @Test
    void testDeprecatedConstructor() {
        // GIVEN
        final double expectedIsp = 10;
        // WHEN
        final ImpulseManeuver maneuver = new ImpulseManeuver(new DateDetector(), null, Vector3D.ZERO,
                expectedIsp, Control3DVectorCostType.NONE);
        // THEN
        Assertions.assertEquals(expectedIsp, maneuver.getIsp());
    }

    @Test
    void testInit() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        final ImpulseProvider impulseProvider = ImpulseProvider.of(Vector3D.PLUS_I);
        final ImpulseManeuver maneuver = new ImpulseManeuver(new DateDetector(), null, impulseProvider,
                1, Control3DVectorCostType.NONE);
        // WHEN
        maneuver.init(mockedState, date);
    }

    @Test
    void testFinish() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        final ImpulseProvider impulseProvider = ImpulseProvider.of(Vector3D.PLUS_I);
        final ImpulseManeuver maneuver = new ImpulseManeuver(new DateDetector(), null, impulseProvider,
                1, Control3DVectorCostType.NONE);
        // WHEN
        maneuver.finish(mockedState);
    }

    @Test
    void testMultipleDates() {
        // GIVEN
        final Orbit initialOrbit = getOrbit();
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        final AbsoluteDate[] dates = new AbsoluteDate[10];
        for (int i = 0; i < dates.length; i++) {
            dates[i] = initialOrbit.getDate().shiftedBy(i * 10 + 1);
        }
        final double isp = Double.POSITIVE_INFINITY;
        final ImpulseManeuver maneuver = new ImpulseManeuver(new DateDetector(dates).withMaxCheck(5), null, new GrowingImpulseProvider(), isp,
                Control3DVectorCostType.NONE);
        propagator.addEventDetector(maneuver);
        // WHEN
        final SpacecraftState actualState = propagator.propagate(dates[dates.length - 1].shiftedBy(1));
        // THEN
        propagator.resetInitialState(new SpacecraftState(initialOrbit));
        propagator.clearEventsDetectors();
        for (int i = 0; i < dates.length; i++) {
            propagator.addEventDetector(new ImpulseManeuver(new DateDetector(dates[i]), Vector3D.PLUS_I.scalarMultiply(i + 1), isp));
        }
        final SpacecraftState expectedState = propagator.propagate(actualState.getDate());
        final Vector3D relativePosition = expectedState.getPosition().subtract(actualState.getPosition());
        Assertions.assertEquals(0., relativePosition.getNorm(), 1e-4);
    }

    private static class GrowingImpulseProvider implements ImpulseProvider {
        int count = 0;

        @Override
        public Vector3D getImpulse(SpacecraftState state, boolean isForward) {
            count++;
            return Vector3D.PLUS_I.scalarMultiply(count);
        }
    }

    private static CartesianOrbit getOrbit() {
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final double initialX = 7100e3;
        final double initialY = 0.0;
        final double initialZ = 1300e3;
        final double initialVx = 0;
        final double initialVy = 8000;
        final double initialVz = 1000;
        final Vector3D position = new Vector3D(initialX, initialY, initialZ);
        final Vector3D velocity = new Vector3D(initialVx, initialVy, initialVz);
        final AbsoluteDate epoch = new AbsoluteDate(2010, 1, 1, 0, 0, 0, TimeScalesFactory.getUTC());
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(epoch, position, velocity, Vector3D.ZERO);
        return new CartesianOrbit(pv, FramesFactory.getEME2000(), mu);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /** Private detector used for testing issue #663. */
    private class InitializationDetector implements EventDetector {

        /** Flag for detector initialization. */
        private boolean initialized;

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            EventDetector.super.init(s0, t);
            this.initialized = true;
        }

        /** {@inheritDoc} */
        @Override
        public double g(SpacecraftState s) {
            return 1;
        }

        @Override
        public EventHandler getHandler() {
            return new StopOnIncreasing();
        }

    }

}
