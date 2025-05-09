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
package org.orekit.propagation.events;

import org.hipparchus.CalculusFieldElement;

/** Base class for adapting an existing detector.
 * <p>
 * This class is intended to be a base class for changing behaviour
 * of a wrapped existing detector. This base class delegates all
 * its methods to the wrapped detector. Classes extending it can
 * therefore override only the methods they want to change.
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 * @param <T> type of the field element
 * @deprecated since 13.0. Use {@link FieldDetectorModifier} instead.
 */
@Deprecated
public class FieldAdapterDetector<T extends CalculusFieldElement<T>> implements FieldDetectorModifier<T> {

    /** Wrapped detector. */
    private final FieldEventDetector<T> detector;

    /** Build an adaptor wrapping an existing detector.
     * @param detector detector to wrap
     */
    public FieldAdapterDetector(final FieldEventDetector<T> detector) {
        this.detector = detector;
    }

    /** Get the wrapped detector.
     * @return wrapped detector
     */
    public FieldEventDetector<T> getDetector() {
        return detector;
    }
}
