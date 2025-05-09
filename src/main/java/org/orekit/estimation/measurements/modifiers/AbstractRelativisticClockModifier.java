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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modifying theoretical measurements with relativistic clock correction.
 * <p>
 * Relativistic clock correction is caused by the motion of the satellite as well as
 * the change in the gravitational potential
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 *
 * @see "Teunissen, Peter, and Oliver Montenbruck, eds. Springer handbook of global navigation
 * satellite systems. Chapter 19.2. Springer, 2017."
 */
public class AbstractRelativisticClockModifier {

    /** Relativistic effect scale factor. */
    private final double s;

    /** Simple constructor. */
    public AbstractRelativisticClockModifier() {
        this.s = -2.0 / (Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT);
    }

    /** Get the name of the effect modifying the measurement.
     * @return name of the effect modifying the measurement
     * @since 13.0
     */
    public String getEffectName() {
        return "clock relativity";
    }

    /** Computes the relativistic clock correction.
     * @param estimated estimated measurement
     * @return the relativistic clock correction in seconds
     */
    protected double relativisticCorrection(final EstimatedMeasurementBase<?> estimated) {
        // Participants
        final TimeStampedPVCoordinates[] pv = estimated.getParticipants();
        // Relativistic clock correction taking into account two-ways measurements
        return pv.length < 3 ?
               s * (dotProduct(pv[0]) - dotProduct(pv[1])) :
               s * (dotProduct(pv[1]) - dotProduct(pv[2]));
    }

    /** Get the scale factor used to compute relativistic effect.
     * @return the scale factor
     */
    protected double getScaleFactor() {
        return s;
    }

    /** Compute the dot-product of position and velocity vectors.
     * @param pv satellite coordinates
     * @return the dot-product of position and velocity vectors
     */
    private static double dotProduct(final TimeStampedPVCoordinates pv) {
        return Vector3D.dotProduct(pv.getPosition(), pv.getVelocity());
    }

}
