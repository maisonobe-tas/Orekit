/* Copyright 2002-2025 Exotrail
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.LUDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.*;
import org.orekit.propagation.events.handlers.*;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

class FieldImpulseManeuverTest {

    private final double mu = Constants.WGS84_EARTH_MU;
    private final Frame inertialFrame = FramesFactory.getGCRF();
    private final Vector3D initialPosition = new Vector3D(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 2000e3,
            100., -1000.);
    private final Vector3D initialVelocity = new Vector3D(-100., 7.5e3, 1.);
    private final double initialMass = 1000.;
    private final double stepSize = 60.;
    private final double isp = 500.;
    private final Vector3D deltaV = new Vector3D(0.1, -0.5, 0.2);
    private final double timeOfFlight = 10000.;
    private final OrbitType orbitType = OrbitType.EQUINOCTIAL;
    private final PositionAngleType positionAngleType = PositionAngleType.ECCENTRIC;
    private final LofOffset attitudeOverride = new LofOffset(inertialFrame, LOFType.QSW);
    private final GradientField gradientField = GradientField.getField(2);
    private final UnivariateDerivative1Field univariateDerivative1Field = new UnivariateDerivative1(0., 0.).getField();
    private final UnivariateDerivative2Field univariateDerivative2Field = new UnivariateDerivative2(0., 0., 0.).getField();
    private final ComplexField complexField = ComplexField.getInstance();
    private enum DetectorType {
        DATE_DETECTOR,
        LATITUDE_CROSSING_DETECTOR,
        ECLIPSE_DETECTOR
    }

    private static class DummyFieldAdditionalDerivatives implements FieldAdditionalDerivativesProvider<Gradient> {

        DummyFieldAdditionalDerivatives() {
            // nothing to do
        }

        @Override
        public String getName() {
            return "dummyDerivativesName";
        }

        @Override
        public int getDimension() {
            return 1;
        }

        @Override
        public FieldCombinedDerivatives<Gradient> combinedDerivatives(FieldSpacecraftState<Gradient> s) {
            final GradientField field = s.getMass().getField();
            Gradient[] pDot = MathArrays.buildArray(field, 1);
            pDot[0] = field.getZero();
            return new FieldCombinedDerivatives<>(pDot, null);
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Deprecated
    @Test
    void testDeprecatedConstructors() {
        // Given
        final Complex zero = complexField.getZero();
        final Complex complexIsp = zero.add(200.);
        final FieldVector3D<Complex> deltaVSat = new FieldVector3D<>(complexField, Vector3D.PLUS_I);
        final FieldAbsoluteDate<Complex> fieldAbsoluteDate = new FieldAbsoluteDate<>(complexField,
                AbsoluteDate.ARBITRARY_EPOCH);
        final FieldDateDetector<Complex> dateDetector = new FieldDateDetector<>(complexField, fieldAbsoluteDate);

        // When
        final FieldImpulseManeuver<Complex> fieldImpulseManeuver = new FieldImpulseManeuver<>(dateDetector, null, deltaVSat, complexIsp,
                Control3DVectorCostType.TWO_NORM);

        // Then
        Assertions.assertEquals(complexIsp, fieldImpulseManeuver.getIsp());
    }

    @Test
    void testComplexConstructors() {
        // Given
        final Complex zero = complexField.getZero();
        final Complex complexIsp = zero.add(200.);
        final FieldVector3D<Complex> deltaVSat = new FieldVector3D<>(complexField, Vector3D.PLUS_I);
        final FieldAbsoluteDate<Complex> fieldAbsoluteDate = new FieldAbsoluteDate<>(complexField,
                AbsoluteDate.ARBITRARY_EPOCH);
        final FieldEventDetectionSettings<Complex> detectionSettings = new FieldEventDetectionSettings<>(ComplexField.getInstance(),
                EventDetectionSettings.getDefaultEventDetectionSettings());
        final FieldDateDetector<Complex> dateDetector = new FieldDateDetector<>(complexField, fieldAbsoluteDate).withDetectionSettings(detectionSettings);

        // When
        final FieldImpulseManeuver<Complex> fieldImpulseManeuver1 = new FieldImpulseManeuver<>(dateDetector, deltaVSat, complexIsp);
        final FieldImpulseManeuver<Complex> fieldImpulseManeuver2 = new FieldImpulseManeuver<>(dateDetector, null, deltaVSat, complexIsp);

        // Then
        Assertions.assertEquals(fieldImpulseManeuver1.getTrigger(), fieldImpulseManeuver2.getTrigger());
        Assertions.assertEquals(fieldImpulseManeuver1.getControl3DVectorCostType(), fieldImpulseManeuver2.getControl3DVectorCostType());
        Assertions.assertEquals(fieldImpulseManeuver1.getDetectionSettings(), dateDetector.getDetectionSettings());
        Assertions.assertEquals(fieldImpulseManeuver1.getAttitudeOverride(), fieldImpulseManeuver2.getAttitudeOverride());
        Assertions.assertEquals(fieldImpulseManeuver1.getIsp(), fieldImpulseManeuver2.getIsp());
    }

    @Test
    void testEventOccurredEventSlopeFilter() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final Binary64Field field = Binary64Field.getInstance();
        final FieldOrbit<Binary64> fieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        final FieldApsideDetector<Binary64> detector = new FieldApsideDetector<>(fieldOrbit);
        final FieldImpulseManeuver<Binary64> maneuver = new FieldImpulseManeuver<>(new FieldEventSlopeFilter<>(detector,
                FilterType.TRIGGER_ONLY_INCREASING_EVENTS), FieldVector3D.getZero(field), Binary64.ONE);
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(fieldOrbit);
        // WHEN & THEN
        propagator.addEventDetector(maneuver);
        Assertions.assertDoesNotThrow(() -> propagator.propagate(fieldOrbit.getDate().shiftedBy(1e5)));
    }

    @Test
    void testWithDetectionSettings() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldDateDetector<Binary64> fieldDateDetector = new FieldDateDetector<>(field);
        final FieldImpulseManeuver<Binary64> fieldImpulseManeuver = new FieldImpulseManeuver<>(fieldDateDetector,
                FieldVector3D.getZero(field), Binary64.ONE);
        final FieldEventDetectionSettings<Binary64> expectedSettings = new FieldEventDetectionSettings<>(
                FieldAdaptableInterval.of(1), new Binary64(2), 3);
        // WHEN
        final FieldImpulseManeuver<Binary64> maneuverWithSettings = fieldImpulseManeuver.withDetectionSettings(expectedSettings);
        // THEN
        Assertions.assertEquals(fieldImpulseManeuver.getAttitudeOverride(), maneuverWithSettings.getAttitudeOverride());
        Assertions.assertEquals(fieldImpulseManeuver.getIsp(), maneuverWithSettings.getIsp());
        Assertions.assertEquals(fieldImpulseManeuver.getFieldImpulseProvider(), maneuverWithSettings.getFieldImpulseProvider());
        Assertions.assertEquals(expectedSettings.getThreshold(), maneuverWithSettings.getThreshold());
        Assertions.assertEquals(expectedSettings.getMaxIterationCount(), maneuverWithSettings.getMaxIterationCount());
        Assertions.assertEquals(expectedSettings.getMaxCheckInterval(), maneuverWithSettings.getMaxCheckInterval());
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void testEventOccurred(final Action action) {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldDateDetector<Binary64> fieldDateDetector = new FieldDateDetector<>(field);
        final Orbit orbit = createOrbit();
        final FieldCountingHandler<Binary64> handler = new FieldCountingHandler<Binary64>(0, action) {
            @Override
            protected boolean doesCount(FieldSpacecraftState<Binary64> state, FieldEventDetector<Binary64> detector, boolean increasing) {
                return true;
            }
        };
        final FieldImpulseManeuver<Binary64> fieldImpulseManeuver = new FieldImpulseManeuver<>(fieldDateDetector.withHandler(handler),
                FieldVector3D.getZero(field), Binary64.ONE);
        // WHEN
        fieldImpulseManeuver.getHandler().eventOccurred(new FieldSpacecraftState<>(new FieldCartesianOrbit<>(field, orbit)),
                fieldImpulseManeuver, true);
        // THEN
        Assertions.assertEquals(1, handler.getCount());
    }

    @Test
    void testResetStateAttitudeOverride() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldDateDetector<Binary64> fieldDateDetector = new FieldDateDetector<>(field);
        final AbsolutePVCoordinates pvCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new Vector3D(1., 2, 3), new Vector3D(4, 5, 6));
        final FieldSpacecraftState<Binary64> expectedState = new FieldSpacecraftState<>(field, new SpacecraftState(pvCoordinates));
        final FieldImpulseManeuver<Binary64> fieldImpulseManeuver = new FieldImpulseManeuver<>(fieldDateDetector,
                new FrameAlignedProvider(pvCoordinates.getFrame()), FieldVector3D.getZero(field), Binary64.ONE);
        // WHEN
        final FieldSpacecraftState<Binary64> actualState = fieldImpulseManeuver.getHandler().resetState(fieldImpulseManeuver, expectedState);
        // THEN
        compareStates(expectedState, actualState);
    }

    @Test
    void testResetState() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldDateDetector<Binary64> fieldDateDetector = new FieldDateDetector<>(field);
        final AbsolutePVCoordinates pvCoordinates = new AbsolutePVCoordinates(FramesFactory.getEME2000(),
                AbsoluteDate.ARBITRARY_EPOCH, new Vector3D(1., 2, 3), new Vector3D(4, 5, 6));
        final FieldSpacecraftState<Binary64> expectedState = new FieldSpacecraftState<>(field, new SpacecraftState(pvCoordinates));
        final FieldImpulseManeuver<Binary64> fieldImpulseManeuver = new FieldImpulseManeuver<>(fieldDateDetector,
                FieldVector3D.getZero(field), Binary64.ONE);
        // WHEN
        final FieldSpacecraftState<Binary64> actualState = fieldImpulseManeuver.getHandler().resetState(fieldImpulseManeuver, expectedState);
        // THEN
        compareStates(expectedState, actualState);
    }

    private static <T extends CalculusFieldElement<T>> void compareStates(final FieldSpacecraftState<T> expectedState,
                                                                          final FieldSpacecraftState<T> actualState) {
        Assertions.assertEquals(expectedState.getDate(), actualState.getDate());
        Assertions.assertEquals(expectedState.getMass(), actualState.getMass());
        Assertions.assertEquals(expectedState.getAttitude(), actualState.getAttitude());
        Assertions.assertEquals(expectedState.getPosition(), actualState.getPosition());
        Assertions.assertEquals(expectedState.getVelocity(),
                actualState.getVelocity());
    }

    @Test
    void testDeltaVNorm() {
        // Given
        final Complex complexIsp = complexField.getOne().add(200.);
        final FieldVector3D<Complex> deltaVSat = new FieldVector3D<>(complexField, Vector3D.PLUS_I);
        final FieldImpulseProvider<Complex> fieldImpulseProvider = FieldImpulseProvider.of(deltaVSat);
        final FieldAbsoluteDate<Complex> fieldAbsoluteDate = new FieldAbsoluteDate<>(complexField,
                AbsoluteDate.ARBITRARY_EPOCH);
        final FieldDateDetector<Complex> dateDetector = new FieldDateDetector<>(complexField, fieldAbsoluteDate);

        // When
        final FieldImpulseManeuver<Complex> fieldImpulseManeuverNorm1 = new FieldImpulseManeuver<>
                (dateDetector.withHandler(new FieldStopOnEvent<>()), null, fieldImpulseProvider, complexIsp, Control3DVectorCostType.ONE_NORM);
        final FieldImpulseManeuver<Complex> fieldImpulseManeuverNorm2 = new FieldImpulseManeuver<>
                (dateDetector.withHandler(new FieldStopOnEvent<>()), null, fieldImpulseProvider, complexIsp, Control3DVectorCostType.TWO_NORM);
        final FieldImpulseManeuver<Complex> fieldImpulseManeuverNormInf = new FieldImpulseManeuver<>
                (dateDetector.withHandler(new FieldStopOnEvent<>()), null, fieldImpulseProvider, complexIsp, Control3DVectorCostType.INF_NORM);

        // Then
        Assertions.assertEquals(Control3DVectorCostType.ONE_NORM, fieldImpulseManeuverNorm1.getControl3DVectorCostType());
        Assertions.assertEquals(Control3DVectorCostType.TWO_NORM, fieldImpulseManeuverNorm2.getControl3DVectorCostType());
        Assertions.assertEquals(Control3DVectorCostType.INF_NORM, fieldImpulseManeuverNormInf.getControl3DVectorCostType());
    }

    @Test
    void testEclipseDetectorDerivativeStructure() {
        templateDetector(new DSFactory(1, 1).getDerivativeField(),
                DetectorType.ECLIPSE_DETECTOR, Control3DVectorCostType.TWO_NORM);
    }

    @Test
    void testEclipseDetectorGradient() {
        templateDetector(gradientField, DetectorType.ECLIPSE_DETECTOR, Control3DVectorCostType.TWO_NORM);
    }

    @Test
    void testEclipseDetectorGradientNormInf() {
        templateDetector(gradientField, DetectorType.ECLIPSE_DETECTOR, Control3DVectorCostType.INF_NORM);
    }

    @Test
    void testDateDetectorComplex() {
        templateDetector(complexField, DetectorType.DATE_DETECTOR, Control3DVectorCostType.TWO_NORM);
    }

    @Test
    void testDateDetectorUnivariateDerivative2() {
        templateDetector(univariateDerivative2Field, DetectorType.DATE_DETECTOR, Control3DVectorCostType.TWO_NORM);
    }

    @Test
    void testDateDetectorGradientNorm1() {
        templateDetector(gradientField, DetectorType.DATE_DETECTOR, Control3DVectorCostType.ONE_NORM);
    }

    @Test
    void testLatitudeCrossingDetectorUnivariateDerivative1() {
        templateDetector(univariateDerivative1Field, DetectorType.LATITUDE_CROSSING_DETECTOR, Control3DVectorCostType.TWO_NORM);
    }

    @Test
    void testLatitudeCrossingDetectorDerivativeStructure() {
        templateDetector(new DSFactory(1, 1).getDerivativeField(),
                DetectorType.LATITUDE_CROSSING_DETECTOR, Control3DVectorCostType.TWO_NORM);
    }

    private <T extends CalculusFieldElement<T>> FieldImpulseManeuver<T> convertManeuver(
            final Field<T> field, final ImpulseManeuver impulseManeuver, final FieldEventHandler<T> fieldHandler) {
        final T fieldIsp = field.getZero().add(impulseManeuver.getIsp());
        final EventDetector detector = impulseManeuver.getTrigger();
        FieldAbstractDetector<?, T> fieldDetector;
        if (detector instanceof DateDetector) {
            fieldDetector = new FieldDateDetector<>(field, new FieldAbsoluteDate<>(field, ((DateDetector) detector).getDate()));
        } else if (detector instanceof LatitudeCrossingDetector) {
            fieldDetector = new FieldLatitudeCrossingDetector<>(field,
                                                                ((LatitudeCrossingDetector) detector).getBody(),
                                                                ((LatitudeCrossingDetector) detector).getLatitude());
        } else if (detector instanceof EclipseDetector) {
            fieldDetector = new FieldEclipseDetector<>(field,
                                                       ((EclipseDetector) detector).getOccultationEngine());
        } else {
            throw new OrekitInternalError(null);
        }

        return new FieldImpulseManeuver<>(fieldDetector.withDetectionSettings(new FieldEventDetectionSettings<>(field, detector.getDetectionSettings()))
                .withHandler(fieldHandler),
                impulseManeuver.getAttitudeOverride(), FieldImpulseProvider.of(impulseManeuver.getImpulseProvider()), fieldIsp, impulseManeuver.getControl3DVectorCostType());
    }

    private <T extends CalculusFieldElement<T>> void templateDetector(final Field<T> field,
                                                                      final DetectorType detectorType,
                                                                      final Control3DVectorCostType control3DVectorCostType) {
        // Given
        final Orbit initialOrbit = createOrbit();
        final NumericalPropagator propagator = createUnperturbedPropagator(initialOrbit, initialMass);
        final FieldNumericalPropagator<T> fieldPropagator = createUnperturbedFieldPropagator(field,
                initialOrbit, propagator.getInitialState().getMass());
        fieldPropagator.setOrbitType(propagator.getOrbitType());
        final AbsoluteDate endOfPropagationDate = propagator.getInitialState().getDate().shiftedBy(timeOfFlight);
        final ImpulseManeuver impulseManeuver = new ImpulseManeuver(
                buildEventDetector(detectorType, propagator).withHandler(new StopOnEvent()),
                attitudeOverride, ImpulseProvider.of(deltaV), isp, control3DVectorCostType);
        propagator.addEventDetector(impulseManeuver);
        fieldPropagator.addEventDetector(convertManeuver(field, impulseManeuver, new FieldStopOnEvent<>()));
        // When
        final SpacecraftState
            terminalState = propagator.propagate(endOfPropagationDate);
        final FieldSpacecraftState<T> fieldTerminalState = fieldPropagator.propagate(new FieldAbsoluteDate<>(field, endOfPropagationDate));
        // Then
        compareStateToConstantOfFieldState(terminalState, fieldTerminalState);
    }

    private AbstractDetector<?> buildEventDetector(final DetectorType detectorType,
                                                   final AbstractPropagator propagator) {
        final CelestialBody earth = CelestialBodyFactory.getEarth();
        final AbstractDetector<?> eventDetector;
        switch (detectorType) {
            case DATE_DETECTOR:
                eventDetector = new DateDetector(propagator.getInitialState().getDate().
                        shiftedBy(timeOfFlight / 2.));
                break;

            case LATITUDE_CROSSING_DETECTOR:

                eventDetector = new LatitudeCrossingDetector(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                        Constants.WGS84_EARTH_FLATTENING, earth.getBodyOrientedFrame()),
                        0.);
                break;

            case ECLIPSE_DETECTOR:
                eventDetector = new EclipseDetector(CelestialBodyFactory.getSun(),Constants.SUN_RADIUS,
                        new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                        Constants.WGS84_EARTH_FLATTENING, earth.getBodyOrientedFrame()));
                break;

            default:
                throw new OrekitInternalError(null);
        }
        return eventDetector;
    }

    private NumericalPropagator createUnperturbedPropagator(final Orbit initialOrbit,
                                                            final double initialMass) {
        final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(stepSize);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(initialOrbit).withMass(initialMass));
        propagator.setOrbitType(orbitType);
        propagator.setPositionAngleType(positionAngleType);
        return propagator;
    }

    private <T extends CalculusFieldElement<T>> FieldNumericalPropagator<T> createUnperturbedFieldPropagator(
            final Field<T> field, final Orbit initialOrbit, final double initialMass) {
        final T fieldStepSize = field.getOne().add(stepSize);
        final ClassicalRungeKuttaFieldIntegrator<T> fieldIntegrator = new ClassicalRungeKuttaFieldIntegrator<>(field,
                fieldStepSize);
        final FieldNumericalPropagator<T> fieldPropagator = new FieldNumericalPropagator<>(field,
                fieldIntegrator);
        final FieldOrbit<T> fieldInitialOrbit = createConstantFieldOrbit(field, initialOrbit);
        final T fieldInitialMass = field.getZero().add(initialMass);
        fieldPropagator.setInitialState(new FieldSpacecraftState<>(fieldInitialOrbit).withMass(fieldInitialMass));
        fieldPropagator.setOrbitType(orbitType);
        fieldPropagator.setPositionAngleType(positionAngleType);
        return fieldPropagator;
    }

    private <T extends CalculusFieldElement<T>> void compareStateToConstantOfFieldState(final SpacecraftState state,
                                                                                        final FieldSpacecraftState<T> fieldState) {
        final Orbit orbit = state.getOrbit();
        final FieldOrbit<T> fieldOrbit = fieldState.getOrbit();
        final double[] orbitAsArray = new double[6];
        final PositionAngleType positionAngle = PositionAngleType.TRUE;
        final OrbitType type = OrbitType.CARTESIAN;
        type.mapOrbitToArray(orbit, positionAngle, orbitAsArray, orbitAsArray.clone());
        final double[] fieldRealOrbitAsArray = orbitAsArray.clone();
        type.mapOrbitToArray(fieldOrbit.toOrbit(), positionAngle, fieldRealOrbitAsArray, fieldRealOrbitAsArray.clone());
        final double tolPos = 5e-2;
        final double tolVel = 3e-5;
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(orbitAsArray[i], fieldRealOrbitAsArray[i], tolPos);
            Assertions.assertEquals(orbitAsArray[i + 3], fieldRealOrbitAsArray[i + 3], tolVel);
        }
        Assertions.assertEquals(state.getMass(), fieldState.getMass().getReal(), 1e-3);

    }

    private CartesianOrbit createOrbit() {
        final PVCoordinates pvCoordinates = new PVCoordinates(initialPosition, initialVelocity);
        return new CartesianOrbit(pvCoordinates, inertialFrame, AbsoluteDate.ARBITRARY_EPOCH, mu);
    }

    private <T extends CalculusFieldElement<T>> FieldCartesianOrbit<T> createConstantFieldOrbit(final Field<T> field,
                                                                                                final Orbit orbit) {
        final PVCoordinates pvCoordinates = orbit.getPVCoordinates();
        final FieldVector3D<T> fieldPosition = new FieldVector3D<>(field, pvCoordinates.getPosition());
        final FieldVector3D<T> fieldVelocity = new FieldVector3D<>(field, pvCoordinates.getVelocity());
        final FieldPVCoordinates<T> fieldPVCoordinates = new FieldPVCoordinates<>(fieldPosition,
                fieldVelocity);
        return new FieldCartesianOrbit<>(fieldPVCoordinates, inertialFrame,
                                         new FieldAbsoluteDate<>(field, orbit.getDate()), field.getZero().add(mu));
    }

    @Test
    void testAdditionalStatePropagation() {
        // Given
        final UnivariateDerivative1 zero = univariateDerivative1Field.getZero();
        final FieldNumericalPropagator<UnivariateDerivative1> fieldPropagator = createFieldPropagatorForAdditionalStatesAndDerivatives(
                univariateDerivative1Field);
        FieldSpacecraftState<UnivariateDerivative1> initialState = fieldPropagator.getInitialState();
        final String name = "dummy";
        initialState = initialState.addAdditionalData(name, zero);
        fieldPropagator.resetInitialState(initialState);
        // When
        final FieldAbsoluteDate<UnivariateDerivative1> targetDate = initialState.getDate().shiftedBy(zero.add(10000.));
        final FieldSpacecraftState<UnivariateDerivative1> terminalState = fieldPropagator.propagate(targetDate);
        // Then
        final UnivariateDerivative1 actualValue = terminalState.getAdditionalState(name)[0];
        Assertions.assertEquals(zero, actualValue);
    }

    @Test
    void testAdditionalStateDerivativesPropagation() {
        // Given
       final Gradient zero = gradientField.getZero();
        final FieldNumericalPropagator<Gradient> fieldPropagator = createFieldPropagatorForAdditionalStatesAndDerivatives(
                gradientField);
        FieldSpacecraftState<Gradient> initialState = fieldPropagator.getInitialState();
        final DummyFieldAdditionalDerivatives additionalDerivatives = new DummyFieldAdditionalDerivatives();
        final String name = additionalDerivatives.getName();
        final Gradient[] dummyState = new Gradient[] { zero };
        fieldPropagator.addAdditionalDerivativesProvider(additionalDerivatives);
        initialState = initialState.addAdditionalData(name, dummyState);
        fieldPropagator.resetInitialState(initialState);
        // When
        final FieldAbsoluteDate<Gradient> targetDate = initialState.getDate().shiftedBy(zero.add(10000.));
        final FieldSpacecraftState<Gradient> terminalState = fieldPropagator.propagate(targetDate);
        // Then
        final Gradient actualValue = terminalState.getAdditionalStatesDerivatives().get(name)[0];
        Assertions.assertEquals(dummyState[0], actualValue);
    }

    private <T extends CalculusFieldElement<T>> FieldNumericalPropagator<T> createFieldPropagatorForAdditionalStatesAndDerivatives(
            final Field<T> field) {
        final Orbit initialOrbit = createOrbit();
        final NumericalPropagator propagator = createUnperturbedPropagator(initialOrbit, initialMass);
        final DateDetector dateDetector = new DateDetector(propagator.getInitialState().getDate().shiftedBy(100.));
        final ImpulseManeuver impulseManeuver = new ImpulseManeuver(dateDetector, Vector3D.PLUS_I, isp);
        final FieldNumericalPropagator<T> fieldPropagator = createUnperturbedFieldPropagator(field,
                initialOrbit, propagator.getInitialState().getMass());
        fieldPropagator.addEventDetector(convertManeuver(field, impulseManeuver, new FieldStopOnEvent<>()));
        return fieldPropagator;
    }

    @Test
    void testBackAndForthPropagation() {
        // Given
        final Orbit initialOrbit = createOrbit();
        final NumericalPropagator propagator = createUnperturbedPropagator(initialOrbit, initialMass);
        final DateDetector dateDetector = new DateDetector(propagator.getInitialState().getDate().shiftedBy(-100.));
        final ImpulseManeuver impulseManeuver = new ImpulseManeuver(dateDetector, Vector3D.PLUS_I, isp);
        final UnivariateDerivative1 zero = univariateDerivative1Field.getZero();
        final FieldNumericalPropagator<UnivariateDerivative1> fieldPropagator = createUnperturbedFieldPropagator(univariateDerivative1Field,
                initialOrbit, initialMass);
        fieldPropagator.setAttitudeProvider(propagator.getAttitudeProvider());
        fieldPropagator.setResetAtEnd(true);
        fieldPropagator.addEventDetector(convertManeuver(univariateDerivative1Field, impulseManeuver, new FieldContinueOnEvent<>()));
        // When
        final UnivariateDerivative1 backwardDuration = zero.add(-10000.);
        final FieldAbsoluteDate<UnivariateDerivative1> fieldEpoch = fieldPropagator.getInitialState().getDate();
        fieldPropagator.propagate(fieldEpoch.shiftedBy(backwardDuration));
        final FieldSpacecraftState<UnivariateDerivative1> actualFieldState = fieldPropagator.propagate(fieldEpoch);
        // Then
        final SpacecraftState expectedState = propagator.getInitialState();
        compareStateToConstantOfFieldState(expectedState, actualFieldState);
    }

    @Test
    void testVersusCartesianStateTransitionMatrix() {
        // Given
        final int freeParameters = 3;
        final GradientField field = GradientField.getField(freeParameters);
        final Orbit initialOrbit = createOrbit();
        final NumericalPropagator propagator = createUnperturbedPropagator(initialOrbit, initialMass);
        final FieldNumericalPropagator<Gradient> fieldPropagator = createUnperturbedFieldPropagator(field,
                initialOrbit, initialMass);
        fieldPropagator.setOrbitType(propagator.getOrbitType());
        final AbsoluteDate endOfPropagationDate = propagator.getInitialState().getDate().shiftedBy(timeOfFlight);
        final DateDetector dateDetector = (DateDetector) buildEventDetector(DetectorType.DATE_DETECTOR, propagator);
        final AttitudeProvider attitudeProvider = new FrameAlignedProvider(propagator.getFrame());
        propagator.addEventDetector(dateDetector);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        final Gradient zero = field.getZero();
        final FieldDateDetector<Gradient> fieldDateDetector =
                        new FieldDateDetector<>(field, new FieldAbsoluteDate<>(field, dateDetector.getDate()));
        final FieldVector3D<Gradient> fieldDeltaV = new FieldVector3D<>(
                Gradient.variable(freeParameters, 0, 0.),
                Gradient.variable(freeParameters, 1, 0.),
                Gradient.variable(freeParameters, 2, 0.));
        fieldPropagator.addEventDetector(new FieldImpulseManeuver<>(fieldDateDetector, attitudeProvider,
                fieldDeltaV, zero.add(isp)));
        final String stmAdditionalName = "stm";
        final MatricesHarvester harvester = propagator.setupMatricesComputation(stmAdditionalName, null, null);
        // When
        final SpacecraftState intermediateState = propagator.propagate(dateDetector.getDate());
        final RealMatrix stm1 = harvester.getStateTransitionMatrix(intermediateState);
        final SpacecraftState terminalState = propagator.propagate(endOfPropagationDate);
        final RealMatrix stm2 = harvester.getStateTransitionMatrix(terminalState);
        final DecompositionSolver decompositionSolver = new LUDecomposition(stm1).getSolver();
        final RealMatrix stm = stm2.multiply(decompositionSolver.getInverse());
        final FieldSpacecraftState<Gradient> fieldTerminalState = fieldPropagator.
                propagate(new FieldAbsoluteDate<>(field, endOfPropagationDate));
        // Then
        final FieldVector3D<Gradient> fieldTerminalPosition = fieldTerminalState.getPosition();
        final FieldVector3D<Gradient> fieldTerminalVelocity = fieldTerminalState.getVelocity();
        final double tolerance = 1e0;
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(stm.getEntry(0, 3 + i),
                    fieldTerminalPosition.getX().getPartialDerivative(i), tolerance);
            Assertions.assertEquals(stm.getEntry(1, 3 + i),
                    fieldTerminalPosition.getY().getPartialDerivative(i), tolerance);
            Assertions.assertEquals(stm.getEntry(2, 3 + i),
                    fieldTerminalPosition.getZ().getPartialDerivative(i), tolerance);
            Assertions.assertEquals(stm.getEntry(3, 3 + i),
                    fieldTerminalVelocity.getX().getPartialDerivative(i), tolerance);
            Assertions.assertEquals(stm.getEntry(4, 3 + i),
                    fieldTerminalVelocity.getY().getPartialDerivative(i), tolerance);
            Assertions.assertEquals(stm.getEntry(5, 3 + i),
                    fieldTerminalVelocity.getZ().getPartialDerivative(i), tolerance);
        }
    }

}
