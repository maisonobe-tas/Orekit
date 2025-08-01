/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.analytical;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AdditionalDataProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.stream.Stream;


class AbstractAnalyticalPropagatorTest {

    @Test
    void testRemoveAdditionalDataProvider() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = getOrbit(date);
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final String name = "a";
        final AdditionalDataProvider<Boolean> dataProvider = new AdditionalDataProvider<Boolean>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Boolean getAdditionalData(SpacecraftState state) {
                return Boolean.TRUE;
            }
        };
        // WHEN & THEN
        propagator.addAdditionalDataProvider(dataProvider);
        Assertions.assertFalse(propagator.getAdditionalDataProviders().isEmpty());
        propagator.removeAdditionalDataProvider(dataProvider.getName());
        Assertions.assertTrue(propagator.getAdditionalDataProviders().isEmpty());
    }

    @Test
    void testInternalEventDetector() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = getOrbit(date);
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final AbsoluteDate interruptingDate = date.shiftedBy(1);
        propagator.setAttitudeProvider(new InterruptingAttitudeProvider(interruptingDate));
        // WHEN
        final SpacecraftState state = propagator.propagate(date.shiftedBy(10.));
        // THEN
        Assertions.assertEquals(state.getDate(), interruptingDate);
    }

    @ParameterizedTest
    @EnumSource(value = Action.class, names = {"RESET_STATE", "RESET_DERIVATIVES"})
    void testReset(final Action action) {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = getOrbit(date);
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final EventHandler handler = (s, detector, increasing) -> action;
        final TestDetector detector = new TestDetector(date.shiftedBy(0.5), handler);
        propagator.addEventDetector(detector);
        // WHEN
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(1.));
        // THEN
        Assertions.assertTrue(detector.resetted);
    }

    private static class TestDetector extends DateDetector {
        boolean resetted = false;

        TestDetector(final AbsoluteDate date, final EventHandler handler) {
            super(EventDetectionSettings.getDefaultEventDetectionSettings(), handler, 1., date);
        }

        @Override
        public void reset(SpacecraftState state, AbsoluteDate target) {
            resetted = true;
        }
    }

    @Test
    void testFinish() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = getOrbit(date);
        final TestAnalyticalPropagator propagator = new TestAnalyticalPropagator(orbit);
        final TestHandler handler = new TestHandler();
        propagator.addEventDetector(new DateDetector().withHandler(handler));
        // WHEN
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(1.));
        // THEN
        Assertions.assertTrue(handler.isFinished);
    }

    private static Orbit getOrbit(final AbsoluteDate date) {
        return new KeplerianOrbit(8000000.0, 0.01, 0.87, 2.44, 0.21, -1.05,
                PositionAngleType.MEAN, FramesFactory.getEME2000(), date, Constants.EIGEN5C_EARTH_MU);
    }

    private static class TestHandler extends ContinueOnEvent {
        boolean isFinished = false;

        @Override
        public void finish(SpacecraftState finalState, EventDetector detector) {
            isFinished = true;
        }
    }

    private static class TestAnalyticalPropagator extends AbstractAnalyticalPropagator {

        private final Orbit orbit;

        protected TestAnalyticalPropagator(Orbit orbit) {
            super(new FrameAlignedProvider(FramesFactory.getGCRF()));
            this.orbit = orbit;
            resetInitialState(new SpacecraftState(orbit));
        }

        @Override
        protected double getMass(AbsoluteDate date) {
            return 1;
        }

        @Override
        protected void resetIntermediateState(SpacecraftState state, boolean forward) {

        }

        @Override
        public Orbit propagateOrbit(AbsoluteDate date) {
            return new CartesianOrbit(orbit.getPVCoordinates(), orbit.getFrame(), date, orbit.getMu());
        }
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
}
