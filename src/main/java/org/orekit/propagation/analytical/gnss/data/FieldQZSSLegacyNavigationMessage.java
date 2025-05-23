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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;

import java.util.function.Function;

/**
 * Container for data contained in a QZSS navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldQZSSLegacyNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldLegacyNavigationMessage<T, QZSSLegacyNavigationMessage> {

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldQZSSLegacyNavigationMessage(final Field<T> field, final QZSSLegacyNavigationMessage original) {
        super(field, original);
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldQZSSLegacyNavigationMessage(final Function<V, T> converter,
                                                                                final FieldQZSSLegacyNavigationMessage<V> original) {
        super(converter, original);
    }

    /** {@inheritDoc} */
    @Override
    public QZSSLegacyNavigationMessage toNonField() {
        return new QZSSLegacyNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, QZSSLegacyNavigationMessage>>
        G changeField(final Function<T, U> converter) {
        return (G) new FieldQZSSLegacyNavigationMessage<>(converter, this);
    }

}
