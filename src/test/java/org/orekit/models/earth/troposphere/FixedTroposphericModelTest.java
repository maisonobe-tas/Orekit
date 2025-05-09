/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.troposphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

public class FixedTroposphericModelTest extends AbstractPathDelayTest<FixedTroposphericDelay> {

    private static final double epsilon = 1e-6;

    @Override
    protected FixedTroposphericDelay buildTroposphericModel(final PressureTemperatureHumidityProvider provider) {
        return FixedTroposphericDelay.getDefaultModel();
    }

    @Test
    @Override
    public void testDelay() {
        doTestDelay(defaultDate, defaultPoint, defaultTrackingCoordinates, null,
                    2.1298, 0.0, 3.4346, 0.0, 3.4346);
    }

    @Test
    @Override
    public void testFieldDelay() {
        doTestDelay(Binary64Field.getInstance(),
                    defaultDate, defaultPoint, defaultTrackingCoordinates, null,
                    2.1298, 0.0, 3.4346, 0.0, 3.4346);
    }

    @Override
    @Test
    public void testFixedHeight() {
        doTestFixedHeight(null);
    }

    @Override
    @Test
    public void testFieldFixedHeight() {
        doTestFieldFixedHeight(Binary64Field.getInstance(), null);
    }

    @Override
    @Test
    public void testFixedElevation() {
        doTestFixedElevation(null);
    }

    @Override
    @Test
    public void testFieldFixedElevation() {
        doTestFieldFixedElevation(Binary64Field.getInstance(), null);
    }

    @Test
    public void testModel() {
        final FixedTroposphericDelay model = buildTroposphericModel(null);
        // check with (artificial) test values from tropospheric-delay.txt
        Assertions.assertEquals(2.4,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(90.0), 0.0),
                                                new GeodeticPoint(0., 0., 0.),
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);
        Assertions.assertEquals(27.4,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(0.0), 0.0),
                                                new GeodeticPoint(0., 0., 0.),
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);

        Assertions.assertEquals(14.3,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(0.0), 0.0),
                                                new GeodeticPoint(0., 0., 5000.),
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);
        Assertions.assertEquals(1.2,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(90.0), 0.0),
                                                new GeodeticPoint(0., 0., 5000.),
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(35.0), 0.0),
                                             new GeodeticPoint(0., 0., 1200.),
                                             null, AbsoluteDate.J2000_EPOCH).getDelay();
        Assertions.assertTrue(Precision.compareTo(delay, 6.4, epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(delay, 3.2, epsilon) > 0);

        // sanity checks
        Assertions.assertEquals(14.3,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(-20.0), 0.0),
                                                new GeodeticPoint(0., 0., 5000.),
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);
        Assertions.assertEquals(1.2,
                                model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(90.0),0.0),
                                                new GeodeticPoint(0., 0., 100000.),
                                                null, AbsoluteDate.J2000_EPOCH).getDelay(),
                                epsilon);
    }

    @Test
    public void testFieldModel() {
        doTestFieldModel(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldModel(final Field<T> field) {
        final FixedTroposphericDelay model = buildTroposphericModel(null);
        final T zero = field.getZero();
        // check with (artificial) test values from tropospheric-delay.txt
        Assertions.assertEquals(2.4,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(90.0)), zero),
                                                new FieldGeodeticPoint<>(zero, zero, zero),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);
        Assertions.assertEquals(27.4,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(0.0)), zero),
                                                new FieldGeodeticPoint<>(zero, zero, zero),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);

        Assertions.assertEquals(14.3,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(0.0)), zero),
                                                new FieldGeodeticPoint<>(zero, zero, zero.add(5000.0)),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);
        Assertions.assertEquals(1.2,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(90.0)), zero),
                                                new FieldGeodeticPoint<>(zero, zero, zero.add(5000.0)),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);

        // interpolation between two elevation angles in the table
        final double delay = model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(35.0)), zero),
                                             new FieldGeodeticPoint<>(zero, zero, zero.add(1200.0)),
                                             null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal();
        Assertions.assertTrue(Precision.compareTo(delay, 6.4, epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(delay, 3.2, epsilon) > 0);

        // sanity checks
        Assertions.assertEquals(14.3,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(-20.0)), zero),
                                                new FieldGeodeticPoint<>(zero, zero, zero.add(5000.0)),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);
        Assertions.assertEquals(1.2,
                                model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(90.0)), zero),
                                                new FieldGeodeticPoint<>(zero, zero, zero.add(100000.0)),
                                                null, FieldAbsoluteDate.getJ2000Epoch(field)).getDelay().getReal(),
                                epsilon);
    }

    @Test
    public void testSymmetry() {
        final FixedTroposphericDelay model = buildTroposphericModel(null);
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final double delay1 = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(elevation), 0.0),
                                                  new GeodeticPoint(0., 0., 100.),
                                                  null, AbsoluteDate.J2000_EPOCH).getDelay();
            final double delay2 = model.pathDelay(new TrackingCoordinates(0.0, FastMath.toRadians(180 - elevation), 0.0),
                                                  new GeodeticPoint(0., 0., 100.),
                                                  null, AbsoluteDate.J2000_EPOCH).getDelay();

            Assertions.assertEquals(delay1, delay2, epsilon);
        }
    }

    @Test
    public void testFieldSymmetry() {
        doTestFieldSymmetry(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestFieldSymmetry(final Field<T> field) {
        final FixedTroposphericDelay model = buildTroposphericModel(null);
        final T zero = field.getZero();
        for (int elevation = 0; elevation < 90; elevation += 10) {
            final T delay1 = model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(elevation)), zero),
                                             new FieldGeodeticPoint<>(zero, zero, zero.add(100.)),
                                             null,
                                             FieldAbsoluteDate.getJ2000Epoch(field)).getDelay();
            final T delay2 = model.pathDelay(new FieldTrackingCoordinates<>(zero, zero.newInstance(FastMath.toRadians(180 - elevation)), zero),
                                             new FieldGeodeticPoint<>(zero, zero, zero.add(100.)),
                                             null,
                                             FieldAbsoluteDate.getJ2000Epoch(field)).getDelay();

            Assertions.assertEquals(delay1.getReal(), delay2.getReal(), epsilon);
        }
    }

    @BeforeAll
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

}
