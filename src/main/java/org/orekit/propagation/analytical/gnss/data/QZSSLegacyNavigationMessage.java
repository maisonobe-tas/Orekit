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
import org.hipparchus.Field;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in a QZSS navigation message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class QZSSLegacyNavigationMessage extends LegacyNavigationMessage<QZSSLegacyNavigationMessage> {

    /** Constructor.
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav, weeks
     *                   are always according to GPS)
     */
    public QZSSLegacyNavigationMessage(final TimeScales timeScales, final SatelliteSystem system) {
        super(GNSSConstants.QZSS_MU, GNSSConstants.QZSS_AV, GNSSConstants.QZSS_WEEK_NB,
              timeScales, system);
    }

    /** Constructor from field instance.
     * @param <T> type of the field elements
     * @param original regular field instance
     */
    public <T extends CalculusFieldElement<T>> QZSSLegacyNavigationMessage(final FieldQZSSLegacyNavigationMessage<T> original) {
        super(original);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends CalculusFieldElement<T>, F extends FieldGnssOrbitalElements<T, QZSSLegacyNavigationMessage>>
        F toField(final Field<T> field) {
        return (F) new FieldQZSSLegacyNavigationMessage<>(field, this);
    }

}
