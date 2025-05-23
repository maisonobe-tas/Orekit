/* Copyright 2022-2025 Romain Serra
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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.MathArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldUnboundedCartesianEnergyNeglectingMassTest {

    @Test
    void testGetFieldHamiltonianContribution() {
        // GIVEN
        final FieldUnboundedCartesianEnergyNeglectingMass<Complex> mockedEnergy = new FieldUnboundedCartesianEnergyNeglectingMass<>("", ComplexField.getInstance());
        final Field<Complex> field = ComplexField.getInstance();
        final Complex[] fieldAdjoint = MathArrays.buildArray(field, 6);
        final Complex fieldMass = Complex.ONE;
        final double mass = fieldMass.getReal();
        final double[] adjoint = new double[fieldAdjoint.length];
        // WHEN
        final Complex fieldContribution = mockedEnergy.getFieldHamiltonianContribution(fieldAdjoint, fieldMass);
        // THEN
        final double contribution = mockedEnergy.toCartesianCost().getHamiltonianContribution(adjoint, mass);
        Assertions.assertEquals(contribution, fieldContribution.getReal());
    }

    @Test
    void testGetFieldThrustAccelerationVector() {
        // GIVEN
        final FieldUnboundedCartesianEnergyNeglectingMass<Binary64> energyNeglectingMass = new FieldUnboundedCartesianEnergyNeglectingMass<>("",
                Binary64Field.getInstance());
        final Binary64[] adjoint = MathArrays.buildArray(Binary64Field.getInstance(), 6);
        adjoint[3] = Binary64.ONE;
        // WHEN
        final FieldVector3D<Binary64> fieldThrustVector = energyNeglectingMass.getFieldThrustAccelerationVector(adjoint, Binary64.ONE);
        // THEN
        final Vector3D thrustVector = energyNeglectingMass.toCartesianCost().getThrustAccelerationVector(new double[] { 0., 0., 0., 1., 0., 0.}, 1.);
        Assertions.assertEquals(thrustVector, fieldThrustVector.toVector3D());
    }

}
