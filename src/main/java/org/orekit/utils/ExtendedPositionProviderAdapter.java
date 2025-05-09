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

package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.frames.TransformProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Adapter from {@link ExtendedPositionProvider} to {@link TransformProvider}.
 * <p>
 * The transform provider is a simple translation from a defining frame such
 * that the origin of the transformed frame corresponds to the moving point.
 * </p>
 * <p>
 * This class is roughly the inverse of {@link FrameAdapter}
 * </p>
 * @see FrameAdapter
 * @since 12.0
 * @author Luc Maisonobe
 */
public class ExtendedPositionProviderAdapter extends Frame {

    /** Simple constructor.
     * @param parent parent frame (must be non-null)
     * @param provider coordinates provider defining the position of origin of the transformed frame
     * @param name name of the frame
     */
    public ExtendedPositionProviderAdapter(final Frame parent,
                                           final ExtendedPositionProvider provider,
                                           final String name) {
        super(parent, new TransformProvider() {

            /** {@inheritDoc} */
            @Override
            public StaticTransform getStaticTransform(final AbsoluteDate date) {
                return StaticTransform.of(date, provider.getPosition(date, parent).negate());
            }

            /** {@inheritDoc} */
            @Override
            public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
                return FieldStaticTransform.of(date, provider.getPosition(date, parent).negate());
            }

            /** {@inheritDoc} */
            @Override
            public Transform getTransform(final AbsoluteDate date) {
                return new Transform(date, provider.getPVCoordinates(date, parent).negate());
            }

            /** {@inheritDoc} */
            @Override
            public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
                return new FieldTransform<>(date, provider.getPVCoordinates(date, parent).negate());
            }

        }, name);
    }

}
