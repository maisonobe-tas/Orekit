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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.GNSSPropagatorBuilder;
import org.orekit.time.TimeScales;

/**
 * Base class for GNSS almanacs.
 * @param <O> type of the orbital elements
 * @author Pascal Parraud
 * @since 11.0
 */
public abstract class AbstractAlmanac<O extends AbstractAlmanac<O>> extends CommonGnssData<O> {

    /**
     * Constructor.
     * @param mu Earth's universal gravitational parameter
     * @param angularVelocity mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle number of weeks in the GNSS cycle
     * @param timeScales      known time scales
     * @param system          satellite system to consider for interpreting week number
     *                        (may be different from real system, for example in Rinex nav, weeks
     *                        are always according to GPS)
     */
    public AbstractAlmanac(final double mu, final double angularVelocity, final int weeksInCycle,
                           final TimeScales timeScales, final SatelliteSystem system) {
        super(mu, angularVelocity, weeksInCycle, timeScales, system);
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param <A> type of the orbital elements (non-field version)
     * @param original regular field instance
     */
    protected <T extends CalculusFieldElement<T>,
               A extends AbstractAlmanac<A>> AbstractAlmanac(final FieldAbstractAlmanac<T, A> original) {
        super(original);
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>
     * The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     *  CIO/2010-based ITRF simple EOP} in the default data context.
     * </p><p>
     * This constructor uses the {@link DataContext#getDefault() default data context}
     * </p>
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator(Frames)
     * @see #getPropagator(Frames, AttitudeProvider, Frame, Frame, double)
     * @since 12.0
     */
    @DefaultDataContext
    public GNSSPropagator getPropagator() {
        return new GNSSPropagatorBuilder(this).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * <p>
     * The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     *  CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     * @param frames set of frames to use
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator()
     * @see #getPropagator(Frames, AttitudeProvider, Frame, Frame, double)
     * @since 13.0
     */
    public GNSSPropagator getPropagator(final Frames frames) {
        return new GNSSPropagatorBuilder(this, frames).build();
    }

    /**
     * Get the propagator corresponding to the navigation message.
     * @param frames set of frames to use
     * @param provider attitude provider
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @param mass spacecraft mass in kg
     * @return the propagator corresponding to the navigation message
     * @see #getPropagator()
     * @see #getPropagator(Frames)
     * @since 13.0
     */
    public GNSSPropagator getPropagator(final Frames frames, final AttitudeProvider provider,
                                        final Frame inertial, final Frame bodyFixed, final double mass) {
        return new GNSSPropagatorBuilder(this, frames).attitudeProvider(provider)
                                                      .eci(inertial)
                                                      .ecef(bodyFixed)
                                                      .mass(mass)
                                                      .build();
    }

}
