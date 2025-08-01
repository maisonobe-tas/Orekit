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
package org.orekit.time;

import org.hipparchus.CalculusFieldElement;

/** This interface represents objects that have a {@link AbsoluteDate}
 * date attached to them.
 * <p>Classes implementing this interface can be stored chronologically
 * in sorted sets using {@link ChronologicalComparator} as the
 * underlying comparator. An example using for {@link org.orekit.orbits.Orbit
 * Orbit} instances is given here:</p>
 * <pre>
 *     SortedSet&lt;Orbit&gt; sortedOrbits =
 *         new TreeSet&lt;Orbit&gt;(new ChronologicalComparator());
 *     sortedOrbits.add(orbit1);
 *     sortedOrbits.add(orbit2);
 *     ...
 * </pre>
 * <p>This interface is also the base interface used to {@link
 * org.orekit.utils.TimeStampedCache cache} series of time-dependent
 * objects for interpolation in a thread-safe manner.</p>
 * @see AbsoluteDate
 * @see ChronologicalComparator
 * @see org.orekit.utils.TimeStampedCache
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 */
public interface FieldTimeStamped<T extends CalculusFieldElement<T>> {

    /** Get the date.
     * @return date attached to the object
     */
    FieldAbsoluteDate<T> getDate();

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the number of seconds physically
     * elapsed between the two instants, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * @param other instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     * if the instance is posterior to the argument)
     * @see FieldAbsoluteDate#durationFrom(FieldAbsoluteDate)
     * @since 12.0
     */
    default T durationFrom(final FieldTimeStamped<T> other) {
        return getDate().durationFrom(other.getDate());
    }

    /** Compute the physically elapsed duration between two instants.
     * @param timeStamped instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     * if the instance is posterior to the argument)
     * @see FieldAbsoluteDate#durationFrom(AbsoluteDate)
     * @since 131
     */
    default T durationFrom(TimeStamped timeStamped) {
        return getDate().durationFrom(timeStamped.getDate());
    }

}
