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

/** Geocentric Coordinate Time.
 * <p>Coordinate time at the center of mass of the Earth.
 * This time scale depends linearly from {@link TTScale Terrestrial Time}.</p>
 * <p>This is intended to be accessed thanks to {@link TimeScales},
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class TCGScale implements TimeScale {

    /** LG rate. */
    private static final double LG_RATE = 6.969290134e-10;

    /** Reference date for TCG.
     * <p>The reference date is such that the four following instants are equal:</p>
     * <ul>
     *   <li>1977-01-01T00:00:32.184 TT</li>
     *   <li>1977-01-01T00:00:32.184 TCG</li>
     *   <li>1977-01-01T00:00:32.184 TCB</li>
     *   <li>1977-01-01T00:00:00.000 TAI</li>
     * </ul>
     */
    private final AbsoluteDate referenceDate;

    /** Offset between TT and TAI scales. */
    private final TimeOffset ttOffset;

    /**
     * Package private constructor for the factory.
     *
     * @param tt  TT time scale.
     * @param tai TAI time scale.
     */
    TCGScale(final TimeScale tt, final TimeScale tai) {
        referenceDate = new AbsoluteDate(1977, 1, 1, tai);
        ttOffset = tt.offsetFromTAI(referenceDate);
    }

    /** {@inheritDoc} */
    @Override
    public TimeOffset offsetFromTAI(final AbsoluteDate date) {
        return ttOffset.add(new TimeOffset(LG_RATE * date.durationFrom(referenceDate)));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        return date.durationFrom(referenceDate).multiply(LG_RATE).add(ttOffset.toDouble());
    }

    /** {@inheritDoc} */
    public String getName() {
        return "TCG";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

}
