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

package org.orekit.estimation.common;

import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.modifiers.Bias;

import java.util.Map;

/** Parser for range measurements.
 * @author Luc Maisonobe
 */
class RangeParser extends MeasurementsParser<Range> {
    /** {@inheritDoc} */
    @Override
    public Range parseFields(final String[] fields,
                             final Map<String, StationData> stations,
                             final PVData pvData,
                             final ObservableSatellite satellite,
                             final Bias<Range> satRangeBias,
                             final Weights weights,
                             final String line,
                             final int lineNumber,
                             final String fileName) {
        checkFields(4, fields, line, lineNumber, fileName);
        final StationData stationData = getStationData(fields[2], stations, line, lineNumber, fileName);
        final Range range = new Range(stationData.getStation(), true,
                                      getDate(fields[0], line, lineNumber, fileName),
                                      Double.parseDouble(fields[3]) * 1000.0,
                                      stationData.getRangeSigma(),
                                      weights.getRangeBaseWeight(),
                                      satellite);
        if (stationData.getRangeBias() != null) {
            range.addModifier(stationData.getRangeBias());
        }
        if (satRangeBias != null) {
            range.addModifier(satRangeBias);
        }
        if (stationData.getRangeTroposphericCorrection() != null) {
            range.addModifier(stationData.getRangeTroposphericCorrection());
        }
        return range;
    }
}
