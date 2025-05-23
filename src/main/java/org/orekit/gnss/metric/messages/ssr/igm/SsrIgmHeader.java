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
package org.orekit.gnss.metric.messages.ssr.igm;

import org.orekit.gnss.metric.messages.ssr.SsrHeader;

/**
 * Container for common data in IGS Generic SSR Message type header.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgmHeader extends SsrHeader {

    /** Number of satellites. */
    private int numberOfSatellites;

    /** Constructor. */
    public SsrIgmHeader() {
        // Nothing to do ...
    }

    /**
     * Get the number of satellites for the current IGM message.
     * @return the number of satellites for the current IGM message
     */
    public int getNumberOfSatellites() {
        return numberOfSatellites;
    }

    /**
     * Set the number of satellites for the current IGM message.
     * @param numberOfSatellites the number of satellites to set
     */
    public void setNumberOfSatellites(final int numberOfSatellites) {
        this.numberOfSatellites = numberOfSatellites;
    }

}
