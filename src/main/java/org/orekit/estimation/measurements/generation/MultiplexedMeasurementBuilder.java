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
package org.orekit.estimation.measurements.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.MultiplexedMeasurement;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;


/** Builder for {@link MultiplexedMeasurement} measurements.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class MultiplexedMeasurementBuilder implements MeasurementBuilder<MultiplexedMeasurement> {

    /** Builders for individual measurements. */
    private final List<MeasurementBuilder<?>> builders;

    /** Satellites related to this builder. */
    private final ObservableSatellite[] satellites;

    /** Modifiers that apply to the measurement.*/
    private final List<EstimationModifier<MultiplexedMeasurement>> modifiers;

    /** Simple constructor.
     * @param builders builders for multiplexed measurements
     */
    public MultiplexedMeasurementBuilder(final List<MeasurementBuilder<?>> builders) {
        this.builders  = builders;
        this.modifiers = new ArrayList<>();

        final List<ObservableSatellite> list = new ArrayList<>();
        for (final MeasurementBuilder<?> builder : builders) {
            for (final ObservableSatellite satellite : builder.getSatellites()) {
                if (!list.contains(satellite)) {
                    list.add(satellite);
                }
            }
        }
        this.satellites = list.toArray(new ObservableSatellite[0]);

    }

    /** {@inheritDoc}
     * <p>
     * This implementation stores the time span of the measurements generation.
     * </p>
     */
    @Override
    public void init(final AbsoluteDate start, final AbsoluteDate end) {
        for (final MeasurementBuilder<?> builder : builders) {
            builder.init(start, end);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addModifier(final EstimationModifier<MultiplexedMeasurement> modifier) {
        modifiers.add(modifier);
    }

    /** {@inheritDoc} */
    @Override
    public List<EstimationModifier<MultiplexedMeasurement>> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }

    /** {@inheritDoc} */
    @Override
    public EstimatedMeasurementBase<MultiplexedMeasurement> build(final AbsoluteDate date,
                                                                  final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {

        // estimate underlying measurements
        final List<EstimatedMeasurementBase<?>> underlyingEstimated = new ArrayList<>(builders.size());
        final List<ObservedMeasurement<?>>      underlyingObserved  = new ArrayList<>(builders.size());
        for (final MeasurementBuilder<?> builder : builders) {
            final EstimatedMeasurementBase<?> built = builder.build(date, interpolators);
            underlyingEstimated.add(built);
            underlyingObserved.add(built.getObservedMeasurement());
        }

        // generate observed multiplexed measurement
        final MultiplexedMeasurement measurement = new MultiplexedMeasurement(underlyingObserved);
        for (final EstimationModifier<MultiplexedMeasurement> modifier : getModifiers()) {
            measurement.addModifier(modifier);
        }

        // generate estimated multiplexed measurement
        final SpacecraftState[] states = new SpacecraftState[satellites.length];
        for (int i = 0; i < underlyingEstimated.size(); ++i) {
            final EstimatedMeasurementBase<?> mI = underlyingEstimated.get(i);
            final SpacecraftState[] statesI = mI.getStates();
            for (int j = 0; j < statesI.length; ++j) {
                states[measurement.getMultiplexedStateIndex(i, j)] = statesI[j];
            }
        }
        return measurement.estimateWithoutDerivatives(states);

    }

    /** {@inheritDoc} */
    @Override
    public ObservableSatellite[] getSatellites() {
        return satellites.clone();
    }

}
