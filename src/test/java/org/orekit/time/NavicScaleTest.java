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
package org.orekit.time;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.utils.Constants;

public class NavicScaleTest {

    @Test
    public void testArbitrary() {
        AbsoluteDate tNavIC =
            new AbsoluteDate(new DateComponents(1999, 3, 4), TimeComponents.H00, TimeScalesFactory.getNavIC());
        AbsoluteDate tUTC =
            new AbsoluteDate(new DateComponents(1999, 3, 3), new TimeComponents(23, 59, 47),
                             TimeScalesFactory.getUTC());
        Assertions.assertEquals(tUTC, tNavIC);
    }

    @Test
    public void testConstant() {
        TimeScale scale = TimeScalesFactory.getNavIC();
        double reference = scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH).toDouble();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            Assertions.assertEquals(reference, scale.offsetFromTAI(date).toDouble(), 1.0e-15);
        }
    }

    @Test
    public void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getNavIC();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = scale.offsetFromTAI(date).toDouble();
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime()).toDouble();
            Assertions.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testDuringLeap() {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = TimeScalesFactory.getNavIC();
        final AbsoluteDate before = new AbsoluteDate(new DateComponents(1983, 6, 30),
                                                     new TimeComponents(23, 59, 59),
                                                     utc);
        final AbsoluteDate during = before.shiftedBy(1.25);
        Assertions.assertEquals(61, utc.minuteDuration(during));
        Assertions.assertEquals(1.0, utc.getLeap(during).toDouble(), 1.0e-10);
        Assertions.assertEquals(60, scale.minuteDuration(during));
        Assertions.assertEquals(0.0, scale.getLeap(during).toDouble(), 1.0e-10);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
