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
package org.orekit.models.earth.atmosphere;

import java.io.Serializable;

import org.orekit.time.AbsoluteDate;


/** Container for solar activity data, compatible with NRLMSISE-00 atmosphere model.
 * <p>
 * This model needs daily and average F10.7 solar fluxes and
 * A<sub>p</sub> geomagnetic indices to compute the local density.
 *
 * @author Pascal Parraud
 */
public interface NRLMSISE00InputParameters extends Serializable {

    /** Gets the available data range minimum date.
     * @return the minimum date.
     */
    AbsoluteDate getMinDate();

    /** Gets the available data range maximum date.
     * @return the maximum date.
     */
    AbsoluteDate getMaxDate();

    /** Get the value of the daily F10.7 solar flux for previous day.
     * @param date the current date
     * @return the daily F10.7 flux for previous day
     */
    double getDailyFlux(AbsoluteDate date);

    /** Get the value of the 81 day average of F10.7 solar flux centered on current day.
     * @param date the current date
     * @return the 81 day average of F10.7 solar flux centered on current day
     */
    double getAverageFlux(AbsoluteDate date);

    /** Get the A<sub>p</sub> geomagnetic indices.
     * <p>
     * A<sub>p</sub> indices are provided as an array such as:
     * <ul>
     * <li>0 → daily A<sub>p</sub></li>
     * <li>1 → 3 hr A<sub>p</sub> index for current time</li>
     * <li>2 → 3 hr A<sub>p</sub> index for 3 hrs before current time</li>
     * <li>3 → 3 hr A<sub>p</sub> index for 6 hrs before current time</li>
     * <li>4 → 3 hr A<sub>p</sub> index for 9 hrs before current time</li>
     * <li>5 → Average of eight 3 hr A<sub>p</sub> indices from 12 to 33 hrs
     *          prior to current time</li>
     * <li>6 → Average of eight 3 hr A<sub>p</sub> indices from 36 to 57 hrs
     *          prior to current time</li>
     * </ul>
     * @param date the current date
     * @return the array of A<sub>p</sub> indices
     */
    double[] getAp(AbsoluteDate date);

}
