/*
 * Licensed to the Hipparchus project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orekit.propagation.events;

import org.hipparchus.ode.nonstiff.AdamsBashforthIntegrator;
import org.hipparchus.ode.nonstiff.AdamsMoultonFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;

/**
 * Test event handling with a {@link NumericalPropagator} and a {@link
 * AdamsBashforthIntegrator}.
 *
 * @author Evan Ward
 */
public class FieldCloseEventsNumericalAMTest extends FieldCloseEventsAbstractTest<Binary64> {

    /** Constructor. */
    public FieldCloseEventsNumericalAMTest() {
        super(Binary64Field.getInstance());
    }

    /**
     * Create a propagator using the {@link #initialOrbit}.
     *
     * @param stepSize of integrator.
     * @return a usable propagator.
     */
    public FieldPropagator<Binary64> getPropagator(double stepSize) {
        double[][] tol = ToleranceProvider.getDefaultToleranceProvider(1).getTolerances(initialOrbit, OrbitType.CARTESIAN);
        final AdamsMoultonFieldIntegrator<Binary64> integrator =
                new AdamsMoultonFieldIntegrator<>(field, 4, stepSize, stepSize, tol[0], tol[1]);
        final DormandPrince853FieldIntegrator<Binary64> starter =
                new DormandPrince853FieldIntegrator<>(
                        field, stepSize / 100, stepSize / 10, tol[0], tol[1]);
        starter.setInitialStepSize(stepSize / 20);
        integrator.setStarterIntegrator(starter);
        final FieldNumericalPropagator<Binary64> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setInitialState(new FieldSpacecraftState<>(initialOrbit));
        propagator.setOrbitType(OrbitType.CARTESIAN);
        return propagator;
    }

}
