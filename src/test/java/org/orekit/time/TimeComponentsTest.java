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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;


public class TimeComponentsTest {

    @Test
    public void testOutOfRangeA() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TimeComponents(-1, 10, 10));
    }

    @Test
    public void testOutOfRangeB() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TimeComponents(24, 10, 10));
    }

    @Test
    public void testOutOfRangeC() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TimeComponents(10, -1, 10));
    }

    @Test
    public void testOutOfRangeD() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TimeComponents(10, 60, 10));
    }

    @Test
    public void testOutOfRangeE() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TimeComponents(10, 10, -1));
    }

    @Test
    public void testOutOfRangeF() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TimeComponents(10, 10, 62));
    }

    @Test
    public void testOutOfRangeG() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TimeComponents(86399, 4.5));
    }

    @Test
    public void testOutOfRangeH() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TimeComponents(0, -1.0));
    }

    @Test
    public void testInRange() {

        TimeComponents time = new TimeComponents(10, 10, 10);
        Assertions.assertEquals(10,   time.getHour());
        Assertions.assertEquals(10,   time.getMinute());
        Assertions.assertEquals(10.0, time.getSecond(), 1.0e-10);

        time = new TimeComponents(0.0);
        Assertions.assertEquals(0.0, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(10, 10, 60.999);
        Assertions.assertEquals(10,   time.getHour());
        Assertions.assertEquals(10,   time.getMinute());
        Assertions.assertEquals(60.999, time.getSecond(), 1.0e-10);

        time = new TimeComponents(43200.0);
        Assertions.assertEquals(43200.0, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(86399.999);
        Assertions.assertEquals(86399.999, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(2, 30, 0, 180);
        Assertions.assertEquals(+9000.0, time.getSecondsInLocalDay(), 1.0e-5);
        Assertions.assertEquals(-1800.0, time.getSecondsInUTCDay(),   1.0e-5);
    }

    @Test
    public void testValues() {
        Assertions.assertEquals(    0.0, new TimeComponents( 0, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(21600.0, new TimeComponents( 6, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(43200.0, new TimeComponents(12, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(64800.0, new TimeComponents(18, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, new TimeComponents(23, 59, 59.9).getSecondsInLocalDay(), 1.0e-10);
    }

    @Test
    public void testString() {
        Assertions.assertEquals("00:00:00.000+00:00", new TimeComponents(0).toString());
        Assertions.assertEquals("06:00:00.000+00:00", new TimeComponents(21600).toString());
        Assertions.assertEquals("12:00:00.000+00:00", new TimeComponents(43200).toString());
        Assertions.assertEquals("18:00:00.000+00:00", new TimeComponents(64800).toString());
        Assertions.assertEquals("23:59:59.899999999994179232+00:00", new TimeComponents(86399.9).toString());
        Assertions.assertEquals("00:00:00.000+10:00", new TimeComponents( 0,  0,  0,    600).toString());
        Assertions.assertEquals("06:00:00.000+10:00", new TimeComponents( 6,  0,  0,    600).toString());
        Assertions.assertEquals("12:00:00.000-04:30", new TimeComponents(12,  0,  0,   -270).toString());
        Assertions.assertEquals("18:00:00.000-04:30", new TimeComponents(18,  0,  0,   -270).toString());
        Assertions.assertEquals("23:59:59.900-04:30", new TimeComponents(23, 59,
                                                                         new TimeOffset(59, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND),
                                                                         -270).toString());
        // test leap seconds
        Assertions.assertEquals("23:59:60.500+00:00", new TimeComponents(new TimeOffset(86399).add(new TimeOffset(0.5)),
                                                                         TimeOffset.SECOND, 61).toString());
        // leap second on 1961 is between 1 and 2 seconds in duration
        Assertions.assertEquals("23:59:61.322817980157729984+00:00", new TimeComponents(new TimeOffset(86399).add(new TimeOffset(0.32281798015773)),
                                                                                        TimeOffset.SECOND.multiply(2), 62).toString());
        // test rounding
        Assertions.assertEquals("23:59:59.999999999985448085+00:00", new TimeComponents(86399.99999999999).toString());
        Assertions.assertEquals("23:59:59.999999999999999889+00:00", new TimeComponents(new TimeOffset(86399).add(new TimeOffset(FastMath.nextDown(1.0))),
                                                                                        TimeOffset.ZERO, 60).toString());
    }

    @Test
    public void testParse() {
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("235959.900").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("23:59:59,900").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("235959.900Z").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900Z").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("235959.900+10").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900+00").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("235959.900-00:12").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900+00:00").getSecondsInLocalDay(), 1.0e-10);
        Assertions.assertEquals(86340.0, TimeComponents.parseTime("23:59").getSecondsInLocalDay(), 1.0e-10);
    }

    @Test
    public void testBadFormat() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TimeComponents.parseTime("23h59m59s"));
    }

    @Test
    public void testLocalTime() {
        Assertions.assertEquals(60, TimeComponents.parseTime("23:59:59+01:00").getMinutesFromUTC());
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testComparisons() {
        TimeComponents[] times = {
                 new TimeComponents( 0,  0,  0.0),
                 new TimeComponents( 0,  0,  1.0e-15),
                 new TimeComponents( 0, 12,  3.0),
                 new TimeComponents(15,  9,  3.0),
                 new TimeComponents(23, 59, 59.0),
                 new TimeComponents(23, 59, 60.0 - 1.0e-12)
        };
        for (int i = 0; i < times.length; ++i) {
            for (int j = 0; j < times.length; ++j) {
                if (times[i].compareTo(times[j]) < 0) {
                    Assertions.assertTrue(times[j].compareTo(times[i]) > 0);
                    Assertions.assertNotEquals(times[i], times[j]);
                    Assertions.assertNotEquals(times[j], times[i]);
                    Assertions.assertTrue(times[i].hashCode() != times[j].hashCode());
                    Assertions.assertTrue(i < j);
                } else if (times[i].compareTo(times[j]) > 0) {
                    Assertions.assertTrue(times[j].compareTo(times[i]) < 0);
                    Assertions.assertNotEquals(times[i], times[j]);
                    Assertions.assertNotEquals(times[j], times[i]);
                    Assertions.assertTrue(times[i].hashCode() != times[j].hashCode());
                    Assertions.assertTrue(i > j);
                } else {
                    Assertions.assertEquals(0, times[j].compareTo(times[i]));
                    Assertions.assertEquals(times[i], times[j]);
                    Assertions.assertEquals(times[j], times[i]);
                    Assertions.assertEquals(times[i].hashCode(), times[j].hashCode());
                    Assertions.assertEquals(i, j);
                }
            }
        }
        Assertions.assertNotEquals(times[0], this);
    }

    @Test
    public void testFromSeconds() {
        // setup
        double zeroUlp  = FastMath.nextUp(0.0);
        double sixtyUlp = FastMath.ulp(60.0);
        double one      =  1.0 - sixtyUlp;
        double sixty    = 60.0 - sixtyUlp;
        double sixtyOne = 61.0 - sixtyUlp;

        // action + verify
        MatcherAssert.assertThat(new TimeComponents(new TimeOffset(0).add(new TimeOffset(0)), TimeOffset.ZERO, 60).getSecond(),
                                 CoreMatchers.is(0.0));
        MatcherAssert.assertThat(new TimeComponents(new TimeOffset(0).add(new TimeOffset(zeroUlp)), TimeOffset.ZERO, 60).getSecond(),
                                 CoreMatchers.is(0.0));
        MatcherAssert.assertThat(new TimeComponents(new TimeOffset(86399).add(new TimeOffset(one)), TimeOffset.ZERO, 60).getSecond(),
                                 CoreMatchers.is(sixty));
        MatcherAssert.assertThat(new TimeComponents(new TimeOffset(86399).add(new TimeOffset(one)), TimeOffset.SECOND, 61).getSecond(),
                                 CoreMatchers.is(sixtyOne));
        // I don't like this NaN behavior, but it matches the 10.1 implementation and
        // GLONASSAnalyticalPropagatorTest relied on it.
        // It seems more logical to throw an out of range exception in this case.
        MatcherAssert.assertThat(new TimeComponents(new TimeOffset(86399).add(new TimeOffset(Double.NaN)), TimeOffset.ZERO, 60).getSecond(),
                                 CoreMatchers.is(Double.NaN));
        MatcherAssert.assertThat(new TimeComponents(new TimeOffset(86399).add(new TimeOffset(Double.NaN)), TimeOffset.ZERO, 60).getMinute(),
                                 CoreMatchers.is(0));
        MatcherAssert.assertThat(new TimeComponents(new TimeOffset(86399).add(new TimeOffset(Double.NaN)), TimeOffset.SECOND, 61).getSecond(),
                                 CoreMatchers.is(Double.NaN));
        MatcherAssert.assertThat(new TimeComponents(new TimeOffset(86399).add(new TimeOffset(Double.NaN)), TimeOffset.SECOND, 61).getMinute(),
                                 CoreMatchers.is(0));

        // check errors
        try {
            new TimeComponents(new TimeOffset(FastMath.nextDown(0)), TimeOffset.ZERO, 60);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            new TimeComponents(new TimeOffset(86399).add(new TimeOffset(1)), TimeOffset.ZERO, 60);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            new TimeComponents(new TimeOffset(86399).add(new TimeOffset(1)), TimeOffset.SECOND, 61);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            new TimeComponents(new TimeOffset(0).add(new TimeOffset(0)), TimeOffset.SECOND.negate(), 59);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            new TimeComponents(new TimeOffset(0).add(new TimeOffset(0)), TimeOffset.SECOND, 59);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
    }

    @Test
    public void testTimeComponentsDouble658() {
        // setup
        double zeroUlp = FastMath.nextUp(0.0);
        double dayUlp = FastMath.ulp(86400.0);

        // action + verify
        check(new TimeComponents(0.0), 0, 0, 0);
        check(new TimeComponents(zeroUlp), 0, 0, 0);
        check(new TimeComponents(86399.5), 23, 59, 59.5);
        check(new TimeComponents(FastMath.nextDown(86400.0)), 23, 59, 60 - dayUlp);
        check(new TimeComponents(86400), 23, 59, 60);
        check(new TimeComponents(FastMath.nextUp(86400.0)), 23, 59, 60 + dayUlp);
        check(new TimeComponents(86400.5), 23, 59, 60.5);
        check(new TimeComponents(FastMath.nextDown(86401.0)), 23, 59, 61 - dayUlp);
        try {
            new TimeComponents(86401);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(86401.0));
        }
        try {
            new TimeComponents(-zeroUlp);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(0.0));
        }
    }

    @Test
    public void testTimeComponentsIntDouble658() {
        // setup
        double zeroUlp  = FastMath.nextUp(0.0);
        double sixtyUlp = FastMath.ulp(60.0);
        double one      =  1.0 - sixtyUlp;
        double sixty    = 60.0 - sixtyUlp;
        double sixtyOne = 61.0 - sixtyUlp;

        // action + verify
        check(new TimeComponents(0, 0.0), 0, 0, 0);
        check(new TimeComponents(0, zeroUlp), 0, 0, 0);
        check(new TimeComponents(86399, 0.5), 23, 59, 59.5);
        check(new TimeComponents(86399, one), 23, 59, sixty);
        check(new TimeComponents(86400, 0.0), 23, 59, 60);
        check(new TimeComponents(86400, sixtyUlp), 23, 59, 60 + sixtyUlp);
        check(new TimeComponents(86400, 0.5), 23, 59, 60.5);
        check(new TimeComponents(86400, one), 23, 59, sixtyOne);
        try {
            new TimeComponents(86401, 0.0);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(86400.0));
        }
        try {
            new TimeComponents(86400, 1.0);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(86400.0));
        }
        try {
            new TimeComponents(0, -1.0e-18);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(-1.0e-18));
        }
        try {
            new TimeComponents(-1, 0.0);
            Assertions.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(-1.0));
        }
    }

    @Test
    public void testNaN() {
        Assertions.assertEquals("01:02:NaN+00:00", new TimeComponents(1, 2, TimeOffset.NaN).toString());
    }

    private void check(final TimeComponents tc, int hour, int minute, double second) {
        MatcherAssert.assertThat(tc.getHour(), CoreMatchers.is(hour));
        MatcherAssert.assertThat(tc.getMinute(), CoreMatchers.is(minute));
        MatcherAssert.assertThat(tc.getSecond(), CoreMatchers.is(second));
    }

}
