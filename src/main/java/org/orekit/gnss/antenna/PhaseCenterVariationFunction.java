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
package org.orekit.gnss.antenna;

/**
 * Model for antennas phase center variations.
 * @author Luc Maisonobe
 * @since 9.2
 */
public interface PhaseCenterVariationFunction {

    /**
     * Evaluate phase center variation in one signal direction.
     * @param polarAngle angle from antenna axial direction
     * (zenith angle for receiver antennas, nadir angle for
     * GNSS satellites antennas)
     * @param azimuthAngle angle around axial direction, counted from +X to +Y
     * (note that this convention is consistent with
     * {@link org.hipparchus.geometry.euclidean.threed.Vector3D#getAlpha()
     * Vector3D.getAlpha()}, but it is <em>different</em> from
     * {@link org.orekit.utils.TrackingCoordinates#getAzimuth() TrackingCoordinates.getAzimuth()},
     * so care must be taken when using this for ground receivers)
     * @return phase center variation in the signal direction (m)
     */
    double value(double polarAngle, double azimuthAngle);

}
