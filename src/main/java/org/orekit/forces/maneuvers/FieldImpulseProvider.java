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
package org.orekit.forces.maneuvers;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.time.FieldAbsoluteDate;

/** Interface providing velocity increment vectors to impulsive maneuvers (Field version).
 *
 * @author Romain Serra
 * @see ImpulseProvider
 * @see FieldImpulseManeuver
 * @since 13.0
 */
public interface FieldImpulseProvider<T extends CalculusFieldElement<T>> {

    /**
     * Method returning the impulse to be applied (Field version).
     * @param state state before the maneuver is applied if {@code isForward} is true, after otherwise
     * @param isForward flag on propagation direction
     * @return impulse in satellite's frame
     */
    FieldVector3D<T> getImpulse(FieldSpacecraftState<T> state, boolean isForward);

    /**
     * Method called at start of propagation.
     * @param initialState state at start of propagation
     * @param targetDate target end date
     */
    default void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> targetDate) {
        // nothing by default
    }

    /**
     * Method called at end of propagation.
     * @param finalState state at end of propagation
     */
    default void finish(final FieldSpacecraftState<T> finalState) {
        // nothing by default
    }

    /**
     * Get a provider returning a given vector for forward propagation and its opposite for backward.
     * @param forwardImpulse forward impulse vector
     * @param <T> field type
     * @return constant provider
     */
    static <T extends CalculusFieldElement<T>> FieldImpulseProvider<T> of(final FieldVector3D<T> forwardImpulse) {
        return (state, isForward) -> isForward ? forwardImpulse : forwardImpulse.negate();
    }

    /**
     * Get a provider returning a given vector for forward propagation and its opposite for backward.
     * @param forwardImpulse forward impulse vector
     * @param field field
     * @param <T> field type
     * @return constant provider
     */
    static <T extends CalculusFieldElement<T>> FieldImpulseProvider<T> of(final Field<T> field,
                                                                          final Vector3D forwardImpulse) {
        return of(new FieldVector3D<>(field, forwardImpulse));
    }

    /**
     * Get a provider from a non-Field version.
     * @param impulseProvider impulse provider
     * @param <T> field type
     * @return provider
     */
    static <T extends CalculusFieldElement<T>> FieldImpulseProvider<T> of(final ImpulseProvider impulseProvider) {
        return (state, isForward) -> {
            final Vector3D deltaV = impulseProvider.getImpulse(state.toSpacecraftState(), isForward);
            return new FieldVector3D<>(state.getDate().getField(), deltaV);
        };
    }
}
