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
package org.orekit.propagation;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.DataDictionary;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.TimeStampedPVCoordinates;

/** This class is the representation of a complete state holding orbit, attitude
 * and mass information at a given date, meant primarily for propagation.
 *
 * <p>It contains an {@link Orbit}, or an {@link AbsolutePVCoordinates} if there
 * is no definite central body, plus the current mass and attitude at the intrinsic
 * {@link AbsoluteDate}. Quantities are guaranteed to be consistent in terms
 * of date and reference frame. The spacecraft state may also contain additional
 * data, which are simply named.
 * </p>
 * <p>
 * The state can be slightly shifted to close dates. This actual shift varies
 * between {@link Orbit} and {@link AbsolutePVCoordinates}.
 * For attitude it is a linear extrapolation taking the spin rate into account
 * and no mass change. It is <em>not</em> intended as a replacement for proper
 * orbit and attitude propagation but should be sufficient for either small
 * time shifts or coarse accuracy.
 * </p>
 * <p>
 * The instance <code>SpacecraftState</code> is guaranteed to be immutable.
 * </p>
 * @see NumericalPropagator
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class SpacecraftState implements TimeStamped, TimeShiftable<SpacecraftState> {

    /** Default mass. */
    public static final double DEFAULT_MASS = 1000.0;

    /**
     * tolerance on date comparison in {@link #checkConsistency(Orbit, Attitude)}. 100 ns
     * corresponds to sub-mm accuracy at LEO orbital velocities.
     */
    private static final double DATE_INCONSISTENCY_THRESHOLD = 100e-9;

    /** Orbital state. */
    private final Orbit orbit;

    /** Trajectory state, when it is not an orbit. */
    private final AbsolutePVCoordinates absPva;

    /** Attitude. */
    private final Attitude attitude;

    /** Current mass (kg). */
    private final double mass;

    /** Additional data, can be any object (String, double[], etc.). */
    private final DataDictionary additional;

    /** Additional states derivatives.
     * @since 11.1
     */
    private final DoubleArrayDictionary additionalDot;

    /** Build a spacecraft state from orbit only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param orbit the orbit
     */
    public SpacecraftState(final Orbit orbit) {
        this(orbit, getDefaultAttitudeProvider(orbit.getFrame())
                        .getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             DEFAULT_MASS, null, null);
    }

    /** Build a spacecraft state from orbit and attitude. Kept for performance.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude)
        throws IllegalArgumentException {
        this(orbit, attitude, DEFAULT_MASS, null, null);
    }

    /** Create a new instance from orbit and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param orbit the orbit
     * @param mass the mass (kg)
     * @deprecated since 13.0, use withXXX
     */
    @Deprecated
    public SpacecraftState(final Orbit orbit, final double mass) {
        this(orbit, getDefaultAttitudeProvider(orbit.getFrame())
                        .getAttitude(orbit, orbit.getDate(), orbit.getFrame()),
             mass, null, null);
    }

    /** Build a spacecraft state from orbit, attitude and mass.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @deprecated since 13.0, use withXXX
     */
    @Deprecated
    public SpacecraftState(final Orbit orbit, final Attitude attitude, final double mass)
        throws IllegalArgumentException {
        this(orbit, attitude, mass, null, null);
    }

    /** Build a spacecraft state from orbit, attitude, mass, additional states and derivatives.
     * @param orbit the orbit
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional data (may be null if no additional states are available)
     * @param additionalDot additional states derivatives (may be null if no additional states derivatives are available)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @since 13.0
     */
    public SpacecraftState(final Orbit orbit, final Attitude attitude, final double mass,
                           final DataDictionary additional, final DoubleArrayDictionary additionalDot)
        throws IllegalArgumentException {
        checkConsistency(orbit, attitude);
        this.orbit      = orbit;
        this.absPva     = null;
        this.attitude   = attitude;
        this.mass       = mass;
        this.additional = additional == null ? new DataDictionary() : additional;
        this.additionalDot = additionalDot == null ? new DoubleArrayDictionary() : new DoubleArrayDictionary(additionalDot);
    }

    /** Build a spacecraft state from position-velocity-acceleration only.
     * <p>Attitude and mass are set to unspecified non-null arbitrary values.</p>
     * @param absPva position-velocity-acceleration
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva) {
        this(absPva, getDefaultAttitudeProvider(absPva.getFrame())
                        .getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             DEFAULT_MASS, null, null);
    }

    /** Build a spacecraft state from position-velocity-acceleration and attitude. Kept for performance.
     * <p>Mass is set to an unspecified non-null arbitrary value.</p>
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final Attitude attitude)
        throws IllegalArgumentException {
        this(absPva, attitude, DEFAULT_MASS, null, null);
    }

    /** Create a new instance from position-velocity-acceleration and mass.
     * <p>Attitude law is set to an unspecified default attitude.</p>
     * @param absPva position-velocity-acceleration
     * @param mass the mass (kg)
     * @deprecated since 13.0, use withXXX
     */
    @Deprecated
    public SpacecraftState(final AbsolutePVCoordinates absPva, final double mass) {
        this(absPva, getDefaultAttitudeProvider(absPva.getFrame())
                        .getAttitude(absPva, absPva.getDate(), absPva.getFrame()),
             mass,  null, null);
    }

    /** Build a spacecraft state from position-velocity-acceleration, attitude and mass.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @deprecated since 13.0, use withXXX
     */
    @Deprecated
    public SpacecraftState(final AbsolutePVCoordinates absPva, final Attitude attitude, final double mass)
        throws IllegalArgumentException {
        this(absPva, attitude, mass, null, null);
    }

    /** Build a spacecraft state from position-velocity-acceleration, attitude, mass and additional states and derivatives.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @param mass the mass (kg)
     * @param additional additional data (may be null if no additional data are available)
     * @param additionalDot additional states derivatives(may be null if no additional states derivatives are available)
     * @exception IllegalArgumentException if orbit and attitude dates
     * or frames are not equal
     * @since 13.0
     */
    public SpacecraftState(final AbsolutePVCoordinates absPva, final Attitude attitude, final double mass,
                           final DataDictionary additional, final DoubleArrayDictionary additionalDot)
        throws IllegalArgumentException {
        checkConsistency(absPva, attitude);
        this.orbit      = null;
        this.absPva     = absPva;
        this.attitude   = attitude;
        this.mass       = mass;
        this.additional = additional == null ? new DataDictionary() : new DataDictionary(additional);
        this.additionalDot = additionalDot == null ? new DoubleArrayDictionary() : new DoubleArrayDictionary(additionalDot);
    }

    /**
     * Create a new instance with input mass.
     * @param newMass mass
     * @return new state
     * @since 13.0
     */
    public SpacecraftState withMass(final double newMass) {
        if (isOrbitDefined()) {
            return new SpacecraftState(orbit, attitude, newMass, additional, additionalDot);
        } else {
            return new SpacecraftState(absPva, attitude, newMass, additional, additionalDot);
        }
    }

    /**
     * Create a new instance with input attitude.
     * @param newAttitude attitude
     * @return new state
     * @since 13.0
     */
    public SpacecraftState withAttitude(final Attitude newAttitude) {
        if (isOrbitDefined()) {
            return new SpacecraftState(orbit, newAttitude, mass, additional, additionalDot);
        } else {
            return new SpacecraftState(absPva, newAttitude, mass, additional, additionalDot);
        }
    }

    /**
     * Create a new instance with input additional data.
     * @param dataDictionary additional data
     * @return new state
     * @since 13.0
     */
    public SpacecraftState withAdditionalData(final DataDictionary dataDictionary) {
        if (isOrbitDefined()) {
            return new SpacecraftState(orbit, attitude, mass, dataDictionary, additionalDot);
        } else {
            return new SpacecraftState(absPva, attitude, mass, dataDictionary, additionalDot);
        }
    }

    /**
     * Create a new instance with input additional data.
     * @param additionalStateDerivatives additional state derivatives
     * @return new state
     * @since 13.0
     */
    public SpacecraftState withAdditionalStatesDerivatives(final DoubleArrayDictionary additionalStateDerivatives) {
        if (isOrbitDefined()) {
            return new SpacecraftState(orbit, attitude, mass, additional, additionalStateDerivatives);
        } else {
            return new SpacecraftState(absPva, attitude, mass, additional, additionalStateDerivatives);
        }
    }

    /** Add an additional data.
     * <p>
     * {@link SpacecraftState SpacecraftState} instances are immutable,
     * so this method does <em>not</em> change the instance, but rather
     * creates a new instance, which has the same orbit, attitude, mass
     * and additional states as the original instance, except it also
     * has the specified state. If the original instance already had an
     * additional data with the same name, it will be overridden. If it
     * did not have any additional state with that name, the new instance
     * will have one more additional state than the original instance.
     * </p>
     * @param name name of the additional data (names containing "orekit"
     * with any case are reserved for the library internal use)
     * @param value value of the additional data
     * @return a new instance, with the additional data added
     * @see #hasAdditionalData(String)
     * @see #getAdditionalData(String)
     * @see #getAdditionalDataValues()
     * @since 13.0
     */
    public SpacecraftState addAdditionalData(final String name, final Object value) {
        final DataDictionary newDict = new DataDictionary(additional);
        if (value instanceof double[]) {
            newDict.put(name, ((double[]) value).clone());
        } else if (value instanceof Double) {
            newDict.put(name, new double[] {(double) value});
        } else {
            newDict.put(name, value);
        }
        return withAdditionalData(newDict);
    }

    /** Add an additional state derivative.
     * <p>
     * {@link SpacecraftState SpacecraftState} instances are immutable,
     * so this method does <em>not</em> change the instance, but rather
     * creates a new instance, which has the same components as the original
     * instance, except it also has the specified state derivative. If the
     * original instance already had an additional state derivative with the
     * same name, it will be overridden. If it did not have any additional
     * state derivative with that name, the new instance will have one more
     * additional state derivative than the original instance.
     * </p>
     * @param name name of the additional state derivative (names containing "orekit"
     * with any case are reserved for the library internal use)
     * @param value value of the additional state derivative
     * @return a new instance, with the additional state added
     * @see #hasAdditionalStateDerivative(String)
     * @see #getAdditionalStateDerivative(String)
     * @see #getAdditionalStatesDerivatives()
     * @since 11.1
     */
    public SpacecraftState addAdditionalStateDerivative(final String name, final double... value) {
        final DoubleArrayDictionary newDict = new DoubleArrayDictionary(additionalDot);
        newDict.put(name, value.clone());
        return withAdditionalStatesDerivatives(newDict);
    }

    /** Check orbit and attitude dates are equal.
     * @param orbit the orbit
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * are not equal
     */
    private static void checkConsistency(final Orbit orbit, final Attitude attitude)
        throws IllegalArgumentException {
        checkDateAndFrameConsistency(attitude, orbit.getDate(), orbit.getFrame());
    }

    /** Defines provider for default Attitude when not passed to constructor.
     * Currently chosen arbitrarily as aligned with input frame.
     * It is also used in {@link FieldSpacecraftState}.
     * @param frame reference frame
     * @return default attitude provider
     * @since 12.0
     */
    static AttitudeProvider getDefaultAttitudeProvider(final Frame frame) {
        return new FrameAlignedProvider(frame);
    }

    /** Check if the state contains an orbit part.
     * <p>
     * A state contains either an {@link AbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link Orbit orbit}.
     * </p>
     * @return true if state contains an orbit (in which case {@link #getOrbit()}
     * will not throw an exception), or false if the state contains an
     * absolut position-velocity-acceleration (in which case {@link #getAbsPVA()}
     * will not throw an exception)
     */
    public boolean isOrbitDefined() {
        return orbit != null;
    }

    /** Check AbsolutePVCoordinates and attitude dates are equal.
     * @param absPva position-velocity-acceleration
     * @param attitude attitude
     * @exception IllegalArgumentException if orbit and attitude dates
     * are not equal
     */
    private static void checkConsistency(final AbsolutePVCoordinates absPva, final Attitude attitude)
        throws IllegalArgumentException {
        checkDateAndFrameConsistency(attitude, absPva.getDate(), absPva.getFrame());
    }

    /** Check attitude frame and epoch.
     * @param attitude attitude
     * @param date epoch to verify
     * @param frame frame to verify
     */
    private static void checkDateAndFrameConsistency(final Attitude attitude, final AbsoluteDate date, final Frame frame) {
        if (FastMath.abs(date.durationFrom(attitude.getDate())) >
                DATE_INCONSISTENCY_THRESHOLD) {
            throw new OrekitIllegalArgumentException(OrekitMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                    date, attitude.getDate());
        }
        if (frame != attitude.getReferenceFrame()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.FRAMES_MISMATCH,
                    frame.getName(),
                    attitude.getReferenceFrame().getName());
        }
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * simple models. For orbits, the model is a Keplerian one if no derivatives
     * are available in the orbit, or Keplerian plus quadratic effect of the
     * non-Keplerian acceleration if derivatives are available. For attitude,
     * a polynomial model is used. Neither mass nor additional states change.
     * Shifting is <em>not</em> intended as a replacement for proper orbit
     * and attitude propagation but should be sufficient for small time shifts
     * or coarse accuracy.
     * </p>
     * <p>
     * As a rough order of magnitude, the following table shows the extrapolation
     * errors obtained between this simple shift method and an {@link
     * NumericalPropagator numerical
     * propagator} for a low Earth Sun Synchronous Orbit, with a 20x20 gravity field,
     * Sun and Moon third bodies attractions, drag and solar radiation pressure.
     * Beware that these results will be different for other orbits.
     * </p>
     * <table border="1">
     * <caption>Extrapolation Error</caption>
     * <tr style="background-color: #ccccff"><th>interpolation time (s)</th>
     * <th>position error without derivatives (m)</th><th>position error with derivatives (m)</th></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px"> 60</td><td>  18</td><td> 1.1</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">120</td><td>  72</td><td> 9.1</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">300</td><td> 447</td><td> 140</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">600</td><td>1601</td><td>1067</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">900</td><td>3141</td><td>3307</td></tr>
     * </table>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * except for the mass and additional states which stay unchanged
     */
    @Override
    public SpacecraftState shiftedBy(final double dt) {
        if (isOrbitDefined()) {
            return new SpacecraftState(orbit.shiftedBy(dt), attitude.shiftedBy(dt),
                                       mass, shiftAdditional(dt), additionalDot);
        } else {
            return new SpacecraftState(absPva.shiftedBy(dt), attitude.shiftedBy(dt),
                                       mass, shiftAdditional(dt), additionalDot);
        }
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * simple models. For orbits, the model is a Keplerian one if no derivatives
     * are available in the orbit, or Keplerian plus quadratic effect of the
     * non-Keplerian acceleration if derivatives are available. For attitude,
     * a polynomial model is used. Neither mass nor additional states change.
     * Shifting is <em>not</em> intended as a replacement for proper orbit
     * and attitude propagation but should be sufficient for small time shifts
     * or coarse accuracy.
     * </p>
     * <p>
     * As a rough order of magnitude, the following table shows the extrapolation
     * errors obtained between this simple shift method and an {@link
     * NumericalPropagator numerical
     * propagator} for a low Earth Sun Synchronous Orbit, with a 20x20 gravity field,
     * Sun and Moon third bodies attractions, drag and solar radiation pressure.
     * Beware that these results will be different for other orbits.
     * </p>
     * <table border="1">
     * <caption>Extrapolation Error</caption>
     * <tr style="background-color: #ccccff"><th>interpolation time (s)</th>
     * <th>position error without derivatives (m)</th><th>position error with derivatives (m)</th></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px"> 60</td><td>  18</td><td> 1.1</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">120</td><td>  72</td><td> 9.1</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">300</td><td> 447</td><td> 140</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">600</td><td>1601</td><td>1067</td></tr>
     * <tr><td style="background-color: #eeeeff; padding:5px">900</td><td>3141</td><td>3307</td></tr>
     * </table>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * except for the mass and additional states which stay unchanged
     * @since 13.0
     */
    @Override
    public SpacecraftState shiftedBy(final TimeOffset dt) {
        if (isOrbitDefined()) {
            return new SpacecraftState(orbit.shiftedBy(dt), attitude.shiftedBy(dt),
                                       mass, shiftAdditional(dt.toDouble()), additionalDot);
        } else {
            return new SpacecraftState(absPva.shiftedBy(dt), attitude.shiftedBy(dt),
                                       mass, shiftAdditional(dt.toDouble()), additionalDot);
        }
    }

    /** Shift additional states.
     * @param dt time shift in seconds
     * @return shifted additional states
     * @since 11.1.1
     */
    private DataDictionary shiftAdditional(final double dt) {

        // fast handling when there are no derivatives at all
        if (additionalDot.size() == 0) {
            return additional;
        }

        // there are derivatives, we need to take them into account in the additional state
        final DataDictionary shifted = new DataDictionary(additional);
        for (final DoubleArrayDictionary.Entry dotEntry : additionalDot.getData()) {
            final DataDictionary.Entry entry = shifted.getEntry(dotEntry.getKey());
            if (entry != null) {
                entry.scaledIncrement(dt, dotEntry);
            }
        }

        return shifted;

    }

    /** Get the absolute position-velocity-acceleration.
     * <p>
     * A state contains either an {@link AbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link Orbit orbit}. Which
     * one is present can be checked using {@link #isOrbitDefined()}.
     * </p>
     * @return absolute position-velocity-acceleration
     * @exception OrekitIllegalStateException if position-velocity-acceleration is null,
     * which mean the state rather contains an {@link Orbit}
     * @see #isOrbitDefined()
     * @see #getOrbit()
     */
    public AbsolutePVCoordinates getAbsPVA() throws OrekitIllegalStateException {
        if (isOrbitDefined()) {
            throw new OrekitIllegalStateException(OrekitMessages.UNDEFINED_ABSOLUTE_PVCOORDINATES);
        }
        return absPva;
    }

    /** Get the current orbit.
     * <p>
     * A state contains either an {@link AbsolutePVCoordinates absolute
     * position-velocity-acceleration} or an {@link Orbit orbit}. Which
     * one is present can be checked using {@link #isOrbitDefined()}.
     * </p>
     * @return the orbit
     * @exception OrekitIllegalStateException if orbit is null,
     * which means the state rather contains an {@link AbsolutePVCoordinates absolute
     * position-velocity-acceleration}
     * @see #isOrbitDefined()
     * @see #getAbsPVA()
     */
    public Orbit getOrbit() throws OrekitIllegalStateException {
        if (orbit == null) {
            throw new OrekitIllegalStateException(OrekitMessages.UNDEFINED_ORBIT);
        }
        return orbit;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return (absPva == null) ? orbit.getDate() : absPva.getDate();
    }

    /** Get the defining frame.
     * @return the frame in which state is defined
     */
    public Frame getFrame() {
        return isOrbitDefined() ? orbit.getFrame() : absPva.getFrame();
    }

    /** Check if an additional data is available.
     * @param name name of the additional data
     * @return true if the additional data is available
     * @see #addAdditionalData(String, Object)
     * @see #getAdditionalState(String)
     * @see #getAdditionalData(String)
     * @see #getAdditionalDataValues()
     */
    public boolean hasAdditionalData(final String name) {
        return additional.getEntry(name) != null;
    }

    /** Check if an additional state derivative is available.
     * @param name name of the additional state derivative
     * @return true if the additional state derivative is available
     * @see #addAdditionalStateDerivative(String, double[])
     * @see #getAdditionalStateDerivative(String)
     * @see #getAdditionalStatesDerivatives()
     * @since 11.1
     */
    public boolean hasAdditionalStateDerivative(final String name) {
        return additionalDot.getEntry(name) != null;
    }

    /** Check if two instances have the same set of additional states available.
     * <p>
     * Only the names and dimensions of the additional states are compared,
     * not their values.
     * </p>
     * @param state state to compare to instance
     * @exception MathIllegalStateException if an additional state does not have
     * the same dimension in both states
     */
    public void ensureCompatibleAdditionalStates(final SpacecraftState state)
        throws MathIllegalStateException {

        // check instance additional states is a subset of the other one
        for (final DataDictionary.Entry entry : additional.getData()) {
            final Object other = state.additional.get(entry.getKey());
            if (other == null || !entry.getValue().getClass().equals(other.getClass())) {
                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA,
                                          entry.getKey());
            }
            if (other instanceof double[] && ((double[]) other).length != ((double[]) entry.getValue()).length) {
                throw new MathIllegalStateException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                    ((double[]) other).length, ((double[]) entry.getValue()).length);
            }
        }

        // check instance additional states derivatives is a subset of the other one
        for (final DoubleArrayDictionary.Entry entry : additionalDot.getData()) {
            final double[] other = state.additionalDot.get(entry.getKey());
            if (other == null) {
                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA,
                                          entry.getKey());
            }
            if (other.length != entry.getValue().length) {
                throw new MathIllegalStateException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                    other.length, entry.getValue().length);
            }
        }

        if (state.additional.size() > additional.size()) {
            // the other state has more additional states
            for (final DataDictionary.Entry entry : state.additional.getData()) {
                if (additional.getEntry(entry.getKey()) == null) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA,
                                              entry.getKey());
                }
            }
        }

        if (state.additionalDot.size() > additionalDot.size()) {
            // the other state has more additional states
            for (final DoubleArrayDictionary.Entry entry : state.additionalDot.getData()) {
                if (additionalDot.getEntry(entry.getKey()) == null) {
                    throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA,
                                              entry.getKey());
                }
            }
        }

    }

    /**
     * Get an additional state.
     *
     * @param name name of the additional state
     * @return value of the additional state
     * @see #hasAdditionalData(String)
     * @see #getAdditionalDataValues()
     */
    public double[] getAdditionalState(final String name) {
        final Object data = getAdditionalData(name);
        if (!(data instanceof double[])) {
            if (data instanceof Double) {
                return new double[] {(double) data};
            } else {
                throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_BAD_TYPE, name);
            }
        }
        return (double[]) data;
    }

    /**
     * Get an additional data.
     *
     * @param name name of the additional state
     * @return value of the additional state
     * @see #addAdditionalData(String, Object)
     * @see #hasAdditionalData(String)
     * @see #getAdditionalDataValues()
     * @since 13.0
     */
    public Object getAdditionalData(final String name) {
        final DataDictionary.Entry entry = additional.getEntry(name);
        if (entry == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA, name);
        }
        return entry.getValue();
    }

    /** Get an additional state derivative.
     * @param name name of the additional state derivative
     * @return value of the additional state derivative
     * @see #addAdditionalStateDerivative(String, double[])
     * @see #hasAdditionalStateDerivative(String)
     * @see #getAdditionalStatesDerivatives()
     * @since 11.1
     */
    public double[] getAdditionalStateDerivative(final String name) {
        final DoubleArrayDictionary.Entry entry = additionalDot.getEntry(name);
        if (entry == null) {
            throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_DATA, name);
        }
        return entry.getValue();
    }

    /** Get an unmodifiable map of additional data.
     * @return unmodifiable map of additional data
     * @see #addAdditionalData(String, Object)
     * @see #hasAdditionalData(String)
     * @see #getAdditionalState(String)
     * @since 11.1
     */
    public DataDictionary getAdditionalDataValues() {
        return additional;
    }

    /** Get an unmodifiable map of additional states derivatives.
     * @return unmodifiable map of additional states derivatives
     * @see #addAdditionalStateDerivative(String, double[])
     * @see #hasAdditionalStateDerivative(String)
     * @see #getAdditionalStateDerivative(String)
     * @since 11.1
     */
    public DoubleArrayDictionary getAdditionalStatesDerivatives() {
        return additionalDot;
    }

    /** Compute the transform from state defining frame to spacecraft frame.
     * <p>The spacecraft frame origin is at the point defined by the orbit
     * (or absolute position-velocity-acceleration), and its orientation is
     * defined by the attitude.</p>
     * @return transform from specified frame to current spacecraft frame
     */
    public Transform toTransform() {
        final TimeStampedPVCoordinates pv = getPVCoordinates();
        return new Transform(pv.getDate(), pv.negate(), attitude.getOrientation());
    }

    /** Compute the static transform from state defining frame to spacecraft frame.
     * @return static transform from specified frame to current spacecraft frame
     * @see #toTransform()
     * @since 12.0
     */
    public StaticTransform toStaticTransform() {
        return StaticTransform.of(getDate(), getPosition().negate(), attitude.getRotation());
    }

    /** Get the position in state definition frame.
     * @return position in state definition frame
     * @since 12.0
     * @see #getPVCoordinates()
     */
    public Vector3D getPosition() {
        return isOrbitDefined() ? orbit.getPosition() : absPva.getPosition();
    }

    /** Get the velocity in state definition frame.
     * @return velocity in state definition frame
     * @since 13.1
     * @see #getPVCoordinates()
     */
    public Vector3D getVelocity() {
        return isOrbitDefined() ? orbit.getVelocity() : absPva.getVelocity();
    }

    /** Get the {@link TimeStampedPVCoordinates} in orbit definition frame.
     * <p>
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedPVCoordinates} if it needs to keep the value for a while.
     * </p>
     * @return pvCoordinates in orbit definition frame
     */
    public TimeStampedPVCoordinates getPVCoordinates() {
        return isOrbitDefined() ? orbit.getPVCoordinates() : absPva.getPVCoordinates();
    }

    /** Get the position in given output frame.
     * @param outputFrame frame in which position should be defined
     * @return position in given output frame
     * @since 12.0
     * @see #getPVCoordinates(Frame)
     */
    public Vector3D getPosition(final Frame outputFrame) {
        return isOrbitDefined() ? orbit.getPosition(outputFrame) : absPva.getPosition(outputFrame);
    }

    /** Get the {@link TimeStampedPVCoordinates} in given output frame.
     * <p>
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached
     * {@link TimeStampedPVCoordinates}, so the caller is responsible to copy it in a separate
     * {@link TimeStampedPVCoordinates} if it needs to keep the value for a while.
     * </p>
     * @param outputFrame frame in which coordinates should be defined
     * @return pvCoordinates in given output frame
     */
    public TimeStampedPVCoordinates getPVCoordinates(final Frame outputFrame) {
        return isOrbitDefined() ? orbit.getPVCoordinates(outputFrame) : absPva.getPVCoordinates(outputFrame);
    }

    /** Get the attitude.
     * @return the attitude.
     */
    public Attitude getAttitude() {
        return attitude;
    }

    /** Gets the current mass.
     * @return the mass (kg)
     */
    public double getMass() {
        return mass;
    }

    @Override
    public String toString() {
        return "SpacecraftState{" +
                "orbit=" + orbit +
                ", attitude=" + attitude +
                ", mass=" + mass +
                ", additional=" + additional +
                ", additionalDot=" + additionalDot +
                '}';
    }
}
