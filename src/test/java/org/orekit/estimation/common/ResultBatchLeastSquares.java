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

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeStampedPVCoordinates;

public class ResultBatchLeastSquares {

    private int numberOfIteration;
    private int numberOfEvaluation;
    private TimeStampedPVCoordinates estimatedPV;
    private StreamingStatistics rangeStat;
    private StreamingStatistics azimStat;
    private StreamingStatistics elevStat;
    ParameterDriversList propagatorParameters  ;
    ParameterDriversList measurementsParameters;
    private RealMatrix covariances;

    ResultBatchLeastSquares(ParameterDriversList  propagatorParameters,
             ParameterDriversList  measurementsParameters,
             int numberOfIteration, int numberOfEvaluation, TimeStampedPVCoordinates estimatedPV,
             StreamingStatistics rangeStat, StreamingStatistics rangeRateStat,
             StreamingStatistics azimStat, StreamingStatistics elevStat,
             StreamingStatistics posStat, StreamingStatistics velStat,
             RealMatrix covariances) {

        this.propagatorParameters   = propagatorParameters;
        this.measurementsParameters = measurementsParameters;
        this.numberOfIteration      = numberOfIteration;
        this.numberOfEvaluation     = numberOfEvaluation;
        this.estimatedPV            = estimatedPV;
        this.rangeStat              =  rangeStat;
        this.azimStat               = azimStat;
        this.elevStat               = elevStat;
        this.covariances            = covariances;
    }

    public int getNumberOfIteration() {
        return numberOfIteration;
    }

    public int getNumberOfEvaluation() {
        return numberOfEvaluation;
    }

    public TimeStampedPVCoordinates getEstimatedPV() {
        return estimatedPV;
    }

    public StreamingStatistics getRangeStat() {
        return rangeStat;
    }

    public StreamingStatistics getAzimStat() {
        return azimStat;
    }

    public StreamingStatistics getElevStat() {
        return elevStat;
    }

    public RealMatrix getCovariances() {
        return covariances;
    }

    public ParameterDriversList getPropagatorParameters() {
        return propagatorParameters;
    }

    public ParameterDriversList getMeasurementsParameters() {
        return measurementsParameters;
    }

}
