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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.propagation.events.intervals.ApsideDetectionAdaptableIntervalFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

class ApsideDetectorTest {

    private Propagator propagator;

    @Test
    void testSimple() {
        EventDetector detector = new ApsideDetector(propagator.getInitialState().getOrbit()).
                                 withMaxCheck(600.0).
                                 withThreshold(1.0e-12).
                                 withHandler(new ContinueOnEvent());

        Assertions.assertEquals(600.0, detector.getMaxCheckInterval().currentInterval(null, true), 1.0e-15);
        Assertions.assertEquals(1.0e-12, detector.getThreshold(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());


        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(30, logger.getLoggedEvents().size());
        for (LoggedEvent e : logger.getLoggedEvents()) {
            KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(e.getState().getOrbit());
            double expected = e.isIncreasing() ? 0.0 : FastMath.PI;
            Assertions.assertEquals(expected, MathUtils.normalizeAngle(o.getMeanAnomaly(), expected), 4.0e-14);
        }

    }

    @Test
    void testFixedMaxCheck() {
        doTestMaxcheck(AdaptableInterval.of(20.0), 4718);
    }

    @Test
    void testAnomalyAwareMaxCheck() {
        doTestMaxcheck(ApsideDetectionAdaptableIntervalFactory.getApsideDetectionAdaptableInterval(), 663);
    }

    private void doTestMaxcheck(final AdaptableInterval maxCheck, int expectedCalls) {
        CountingApsideDetectorModifier detector = new CountingApsideDetectorModifier(maxCheck);
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(30, logger.getLoggedEvents().size());
        Assertions.assertEquals(expectedCalls, detector.count);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(506.0, 943.0, 7450);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(position,  velocity),
                                               FramesFactory.getEME2000(), date,
                                               Constants.EIGEN5C_EARTH_MU);

        propagator =
            new EcksteinHechlerPropagator(orbit,
                                          Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                          Constants.EIGEN5C_EARTH_MU,
                                          Constants.EIGEN5C_EARTH_C20,
                                          Constants.EIGEN5C_EARTH_C30,
                                          Constants.EIGEN5C_EARTH_C40,
                                          Constants.EIGEN5C_EARTH_C50,
                                          Constants.EIGEN5C_EARTH_C60);
    }

    private class CountingApsideDetectorModifier implements DetectorModifier {

        private final ApsideDetector detector;
        private int count;
        
        public CountingApsideDetectorModifier(final AdaptableInterval maxCheck) {
            this.detector = new ApsideDetector(propagator.getInitialState().getOrbit()).
                  withMaxCheck(maxCheck).
                  withThreshold(1.0e-12).
                  withHandler(new ContinueOnEvent());
        }

        @Override
        public ApsideDetector getDetector() {
            return detector;
        }

        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            DetectorModifier.super.init(s0, t);
            count = 0;
        }

        @Override
        public double g(final SpacecraftState s) {
            ++count;
            return DetectorModifier.super.g(s);
        }

    }

}

