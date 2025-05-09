/* Copyright 2022-2025 Luc Maisonobe
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

package org.orekit.propagation.events.intervals;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;

/** This interface represents an event checking interval that depends on state.
*
* @see FieldEventDetector
* @author Luc Maisonobe
* @since 12.0
* @param <T> the type of the field elements
*/
@FunctionalInterface
public interface FieldAdaptableInterval<T extends CalculusFieldElement<T>> {

    /**
     * Get the current value of maximal time interval between events handler checks.
     *
     * @param state     current state
     * @param isForward direction of propagation
     * @return current value of maximal time interval between events handler checks (only as a double)
     */
    double currentInterval(FieldSpacecraftState<T> state, boolean isForward);

    /**
     * Method creating a constant interval provider.
     * @param <T> field type
     * @param constantInterval value of constant interval
     * @return adaptable interval ready to be added to an event detector
     * @since 12.1
     */
    static <T extends CalculusFieldElement<T>> FieldAdaptableInterval<T> of(final double constantInterval) {
        return (state, isForward) -> constantInterval;
    }

    /**
     * Method creating an interval provider from a non-Field one.
     * @param <T> field type
     * @param adaptableInterval non-Field interval
     * @return adaptable interval ready to be added to an event detector
     * @since 13.0
     */
    static <T extends CalculusFieldElement<T>> FieldAdaptableInterval<T> of(final AdaptableInterval adaptableInterval) {
        return (state, isForward) -> adaptableInterval.currentInterval(state.toSpacecraftState(), isForward);
    }

    /**
     * Method creating an interval taking the minimum value of all candidates.
     * @param defaultMaxCheck default value if no intervals is given as inputv
     * @param adaptableIntervals intervals
     * @param <T> field type
     * @return adaptable interval ready to be added to an event detector
     * @since 13.0
     */
    @SafeVarargs
    static <T extends CalculusFieldElement<T>> FieldAdaptableInterval<T> of(final double defaultMaxCheck,
                                                                            final FieldAdaptableInterval<T>... adaptableIntervals) {
        return (state, isForward) -> {
            double maxCheck = defaultMaxCheck;
            for (final FieldAdaptableInterval<T> interval : adaptableIntervals) {
                maxCheck = FastMath.min(maxCheck, interval.currentInterval(state, isForward));
            }
            return maxCheck;
        };
    }
}

