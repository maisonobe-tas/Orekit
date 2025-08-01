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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import java.util.concurrent.TimeUnit;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

class AbsoluteDateTest {

    @Test
    @DefaultDataContext
    void testStandardEpoch() {
        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale tt  = TimeScalesFactory.getTT();
        Assertions.assertEquals(-210866760000000L, AbsoluteDate.JULIAN_EPOCH.toDate(tt).getTime());
        Assertions.assertEquals(-3506716800000L,   AbsoluteDate.MODIFIED_JULIAN_EPOCH.toDate(tt).getTime());
        Assertions.assertEquals(-631152000000L,    AbsoluteDate.FIFTIES_EPOCH.toDate(tt).getTime());
        Assertions.assertEquals(-378691200000L,    AbsoluteDate.CCSDS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(935280019000L,     AbsoluteDate.GALILEO_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(315964819000L,     AbsoluteDate.GPS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(315964819000L,     AbsoluteDate.QZSS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(1136073633000L,    AbsoluteDate.BEIDOU_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(820443629000L,     AbsoluteDate.GLONASS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(935280019000L,     AbsoluteDate.NAVIC_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(946728000000L,     AbsoluteDate.J2000_EPOCH.toDate(tt).getTime());
    }

    @Test
    @DefaultDataContext
    void testStandardEpochStrings() {
        Assertions.assertEquals("-4712-01-01T12:00:00.000",
                     AbsoluteDate.JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1858-11-17T00:00:00.000",
                     AbsoluteDate.MODIFIED_JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1950-01-01T00:00:00.000",
                            AbsoluteDate.FIFTIES_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1958-01-01T00:00:00.000",
                            AbsoluteDate.CCSDS_EPOCH.toString(TimeScalesFactory.getTAI()));
        Assertions.assertEquals("1999-08-21T23:59:47.000",
                            AbsoluteDate.GALILEO_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1980-01-06T00:00:00.000",
                            AbsoluteDate.GPS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1980-01-06T00:00:00.000",
                            AbsoluteDate.QZSS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("2006-01-01T00:00:00.000",
                            AbsoluteDate.BEIDOU_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1995-12-31T21:00:00.000",
                            AbsoluteDate.GLONASS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1999-08-21T23:59:47.000",
                AbsoluteDate.NAVIC_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1970-01-01T00:00:00.000",
                     AbsoluteDate.JAVA_EPOCH.toString(TimeScalesFactory.getUTC()));
    }

    @Test
    void testJulianEpochRate() {

        for (int i = 0; i < 10; ++i) {
            AbsoluteDate j200i = AbsoluteDate.createJulianEpoch(2000.0 + i);
            AbsoluteDate j2000 = AbsoluteDate.J2000_EPOCH;
            double expected    = i * Constants.JULIAN_YEAR;
            Assertions.assertEquals(expected, j200i.durationFrom(j2000), 4.0e-15 * expected);
        }

    }

    @Test
    void testBesselianEpochRate() {

        for (int i = 0; i < 10; ++i) {
            AbsoluteDate b195i = AbsoluteDate.createBesselianEpoch(1950.0 + i);
            AbsoluteDate b1950 = AbsoluteDate.createBesselianEpoch(1950.0);
            double expected    = i * Constants.BESSELIAN_YEAR;
            Assertions.assertEquals(expected, b195i.durationFrom(b1950), 4.0e-15 * expected);
        }

    }

    @Test
    void testLieske() {

        // the following test values correspond to table 1 in the paper:
        // Precession Matrix Based on IAU (1976) System of Astronomical Constants,
        // Jay H. Lieske, Astronomy and Astrophysics, vol. 73, no. 3, Mar. 1979, p. 282-284
        // http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&defaultprint=YES&filetype=.pdf

        // published table, with limited accuracy
        final double publishedEpsilon = 1.0e-6 * Constants.JULIAN_YEAR;
        checkEpochs(1899.999142, 1900.000000, publishedEpsilon);
        checkEpochs(1900.000000, 1900.000858, publishedEpsilon);
        checkEpochs(1950.000000, 1949.999790, publishedEpsilon);
        checkEpochs(1950.000210, 1950.000000, publishedEpsilon);
        checkEpochs(2000.000000, 1999.998722, publishedEpsilon);
        checkEpochs(2000.001278, 2000.000000, publishedEpsilon);

        // recomputed table, using directly Lieske formulas (i.e. *not* Orekit implementation) with high accuracy
        final double accurateEpsilon = 1.2e-13 * Constants.JULIAN_YEAR;
        checkEpochs(1899.99914161068724704, 1900.00000000000000000, accurateEpsilon);
        checkEpochs(1900.00000000000000000, 1900.00085837097878165, accurateEpsilon);
        checkEpochs(1950.00000000000000000, 1949.99979044229979466, accurateEpsilon);
        checkEpochs(1950.00020956217615449, 1950.00000000000000000, accurateEpsilon);
        checkEpochs(2000.00000000000000000, 1999.99872251362080766, accurateEpsilon);
        checkEpochs(2000.00127751366506194, 2000.00000000000000000, accurateEpsilon);

    }

    private void checkEpochs(final double besselianEpoch, final double julianEpoch, final double epsilon) {
        final AbsoluteDate b = AbsoluteDate.createBesselianEpoch(besselianEpoch);
        final AbsoluteDate j = AbsoluteDate.createJulianEpoch(julianEpoch);
        Assertions.assertEquals(0.0, b.durationFrom(j), epsilon);
    }

    @Test
    void testParse() {
        Assertions.assertEquals(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                            new AbsoluteDate("1858-W46-3", TimeScalesFactory.getTT()));
        Assertions.assertEquals(AbsoluteDate.JULIAN_EPOCH,
                            new AbsoluteDate("-4712-01-01T12:00:00.000", TimeScalesFactory.getTT()));
        Assertions.assertEquals(AbsoluteDate.FIFTIES_EPOCH,
                            new AbsoluteDate("1950-01-01", TimeScalesFactory.getTT()));
        Assertions.assertEquals(AbsoluteDate.CCSDS_EPOCH,
                            new AbsoluteDate("1958-001", TimeScalesFactory.getTAI()));
    }

    @Test
    void testLocalTimeParsing() {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assertions.assertEquals(new AbsoluteDate("2011-12-31T23:00:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+04:30", utc));
        Assertions.assertEquals(new AbsoluteDate("2011-12-31T23:00:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+0430",  utc));
        Assertions.assertEquals(new AbsoluteDate("2011-12-31T23:30:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+04",    utc));
        Assertions.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-07:00", utc));
        Assertions.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-0700",  utc));
        Assertions.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-07",    utc));
    }

    @Test
    void testTimeZoneDisplay() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date = new AbsoluteDate("2000-01-01T01:01:01.000", utc);
        Assertions.assertEquals("2000-01-01T01:01:01.000Z",      date.toString());
        Assertions.assertEquals("2000-01-01T11:01:01.000+10:00", date.toString( 600));
        Assertions.assertEquals("1999-12-31T23:01:01.000-02:00", date.toString(-120));
        Assertions.assertEquals("2000-01-01T01:01:01.000+00:00", date.toString(0));

        // winter time, Europe is one hour ahead of UTC
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Assertions.assertEquals("2001-01-22T11:30:00.000+01:00",
                            new AbsoluteDate("2001-01-22T10:30:00", utc).toString(tz));

        // summer time, Europe is two hours ahead of UTC
        Assertions.assertEquals("2001-06-23T11:30:00.000+02:00",
                            new AbsoluteDate("2001-06-23T09:30:00", utc).toString(tz));

        // check with UTC
        tz = TimeZone.getTimeZone("UTC");
        Assertions.assertEquals("2001-06-23T09:30:00.000+00:00",
                new AbsoluteDate("2001-06-23T09:30:00", utc).toString(tz));

    }

    @Test
    void testLocalTimeLeapSecond() {

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        for (int minutesFromUTC = -1500; minutesFromUTC < 1500; ++minutesFromUTC) {
            DateTimeComponents dtcBeforeLeap = beforeLeap.getComponents(minutesFromUTC);
            DateTimeComponents dtcInsideLeap = inLeap.getComponents(minutesFromUTC);
            Assertions.assertEquals(dtcBeforeLeap.getDate(), dtcInsideLeap.getDate());
            Assertions.assertEquals(dtcBeforeLeap.getTime().getHour(), dtcInsideLeap.getTime().getHour());
            Assertions.assertEquals(dtcBeforeLeap.getTime().getMinute(), dtcInsideLeap.getTime().getMinute());
            Assertions.assertEquals(minutesFromUTC, dtcBeforeLeap.getTime().getMinutesFromUTC());
            Assertions.assertEquals(minutesFromUTC, dtcInsideLeap.getTime().getMinutesFromUTC());
            Assertions.assertEquals(59.8, dtcBeforeLeap.getTime().getSecond(), 1.0e-10);
            Assertions.assertEquals(60.5, dtcInsideLeap.getTime().getSecond(), 1.0e-10);
        }

    }

    @Test
    void testTimeZoneLeapSecond() {

        TimeScale utc = TimeScalesFactory.getUTC();
        final TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        AbsoluteDate localBeforeMidnight = new AbsoluteDate("2012-06-30T21:59:59.800", utc);
        Assertions.assertEquals("2012-06-30T23:59:59.800+02:00",
                            localBeforeMidnight.toString(tz));
        Assertions.assertEquals("2012-07-01T00:00:00.800+02:00",
                            localBeforeMidnight.shiftedBy(1.0).toString(tz));

        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        Assertions.assertEquals("2012-07-01T01:59:59.800+02:00", beforeLeap.toString(tz));
        Assertions.assertEquals("2012-07-01T01:59:60.500+02:00", inLeap.toString(tz));

    }

    @Test
    void testParseLeap() {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        Assertions.assertEquals("2012-06-30T23:59:60.500", inLeap.toString(utc));
    }

    @Test
    void testOutput() {
        TimeScale tt = TimeScalesFactory.getTT();
        Assertions.assertEquals("1950-01-01T01:01:01.000",
                            AbsoluteDate.FIFTIES_EPOCH.shiftedBy(3661.0).toString(tt));
        Assertions.assertEquals("2000-01-01T13:01:01.000",
                            AbsoluteDate.J2000_EPOCH.shiftedBy(3661.0).toString(tt));
    }

    @Test
    void testJ2000() {
        Assertions.assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("2000-01-01T11:59:27.816",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTAI()));
        Assertions.assertEquals("2000-01-01T11:58:55.816",
                     AbsoluteDate.J2000_EPOCH.toString(utc));
    }

    @Test
    void testFraction() {
        AbsoluteDate d =
            new AbsoluteDate(new DateComponents(2000, 1, 1), new TimeComponents(11, 59, 27.816),
                             TimeScalesFactory.getTAI());
        Assertions.assertEquals(0, d.durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

    @Test
    void testScalesOffset() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2006, 2, 24),
                                             new TimeComponents(15, 38, 0),
                                             utc);
        Assertions.assertEquals(33,
                     date.timeScalesOffset(TimeScalesFactory.getTAI(), utc),
                     1.0e-10);
    }

    @Test
    void testUTC() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2002, 1, 1),
                                             new TimeComponents(0, 0, 1),
                                             utc);
        Assertions.assertEquals("2002-01-01T00:00:01.000Z", date.toString());
    }

    @Test
    void test1970() {
        AbsoluteDate date = new AbsoluteDate(new Date(0L), utc);
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", date.toString());
    }

    @Test
    void test1970Instant() {
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new AbsoluteDate(Instant.EPOCH, utc).toString());
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new AbsoluteDate(Instant.ofEpochMilli(0L), utc).toString());
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new AbsoluteDate(Instant.EPOCH, (UTCScale) utc).toString());
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new AbsoluteDate(Instant.ofEpochMilli(0L), (UTCScale) utc).toString());
    }

    @Test
    void testInstantAccuracy() {
        Assertions.assertEquals("1970-01-02T00:16:40.123456789Z", new AbsoluteDate(Instant.ofEpochSecond(87400, 123456789), utc).toString());
        Assertions.assertEquals("1970-01-07T00:10:00.123456789Z", new AbsoluteDate(Instant.ofEpochSecond(519000, 123456789), utc).toString());
        Assertions.assertEquals("1970-01-02T00:16:40.123456789Z", new AbsoluteDate(Instant.ofEpochSecond(87400, 123456789), (UTCScale) utc).toString());
        Assertions.assertEquals("1970-01-07T00:10:00.123456789Z", new AbsoluteDate(Instant.ofEpochSecond(519000, 123456789), (UTCScale) utc).toString());
    }

    @Test
    void testToInstant() {
        Assertions.assertEquals(Instant.ofEpochSecond(0), new AbsoluteDate("1970-01-01T00:00:00.000Z", utc).toInstant());
        Assertions.assertEquals(Instant.ofEpochSecond(0), new AbsoluteDate("1970-01-01T00:00:00.000Z", utc).toInstant(TimeScalesFactory.getTimeScales()));

        Instant expectedInstant = Instant.ofEpochSecond(519000, 123456789);
        Assertions.assertEquals(expectedInstant, new AbsoluteDate("1970-01-07T00:10:00.123456789Z", utc).toInstant());
        Assertions.assertEquals(expectedInstant, new AbsoluteDate("1970-01-07T00:10:00.123456789Z", utc).toInstant(TimeScalesFactory.getTimeScales()));

        Assertions.assertEquals(OffsetDateTime.parse("2024-05-15T09:32:36.123456789Z", DateTimeFormatter.ISO_DATE_TIME).toInstant(),
            new AbsoluteDate("2024-05-15T09:32:36.123456789Z", utc).toInstant());
        Assertions.assertEquals(OffsetDateTime.parse("2024-05-15T09:32:36.123456789Z", DateTimeFormatter.ISO_DATE_TIME).toInstant(),
            new AbsoluteDate("2024-05-15T09:32:36.123456789Z", utc).toInstant(TimeScalesFactory.getTimeScales()));
    }

    @Test
    void testUtcGpsOffset() {
        AbsoluteDate date1   = new AbsoluteDate(new DateComponents(2005, 8, 9),
                                                new TimeComponents(16, 31, 17),
                                                utc);
        AbsoluteDate date2   = new AbsoluteDate(new DateComponents(2006, 8, 9),
                                                new TimeComponents(16, 31, 17),
                                                utc);
        AbsoluteDate dateRef = new AbsoluteDate(new DateComponents(1980, 1, 6),
                                                TimeComponents.H00,
                                                utc);

        // 13 seconds offset between GPS time and UTC in 2005
        long noLeapGap = ((9347 * 24 + 16) * 60 + 31) * 60 + 17;
        long realGap   = (long) date1.durationFrom(dateRef);
        Assertions.assertEquals(13L, realGap - noLeapGap);

        // 14 seconds offset between GPS time and UTC in 2006
        noLeapGap = ((9712 * 24 + 16) * 60 + 31) * 60 + 17;
        realGap   = (long) date2.durationFrom(dateRef);
        Assertions.assertEquals(14L, realGap - noLeapGap);

    }

    @Test
    void testMJDDate() {
        AbsoluteDate dateA = AbsoluteDate.createMJDDate(51544, 0.5 * Constants.JULIAN_DAY,
                                                             TimeScalesFactory.getTT());
        Assertions.assertEquals(0.0, AbsoluteDate.J2000_EPOCH.durationFrom(dateA), 1.0e-15);
        AbsoluteDate dateB = AbsoluteDate.createMJDDate(53774, 0.0, TimeScalesFactory.getUTC());
        AbsoluteDate dateC = new AbsoluteDate("2006-02-08T00:00:00", TimeScalesFactory.getUTC());
        Assertions.assertEquals(0.0, dateC.durationFrom(dateB), 1.0e-15);
    }

    @Test
    void testJDDate() {
        final AbsoluteDate date = AbsoluteDate.createJDDate(2400000, 0.5 * Constants.JULIAN_DAY,
                                                            TimeScalesFactory.getTT());
        Assertions.assertEquals(0.0, AbsoluteDate.MODIFIED_JULIAN_EPOCH.durationFrom(date), 1.0e-15);
    }

    /** Test issue 1310: get a date from a JD using a pivot timescale. */
    @Test
    void testIssue1310JDDateInTDB() {
        // Given
        // -----
        final TDBScale TDBscale = TimeScalesFactory.getTDB();
        final AbsoluteDate refDate = new AbsoluteDate("2023-08-01T00:00:00.000", TDBscale);

        // When
        // ----
        final AbsoluteDate wrongDate  = AbsoluteDate.createJDDate(2460157,
                Constants.JULIAN_DAY / 2.0d, TDBscale);
        final AbsoluteDate properDate = AbsoluteDate.createJDDate(2460157,
                Constants.JULIAN_DAY/2.0d, TDBscale, TimeScalesFactory.getTT());

        // Then
        // ----

        // Wrong date is too far from reference date
        Assertions.assertEquals(0.0, wrongDate.durationFrom(refDate), 1.270e-05);

        // Proper date is close enough from reference date
        Assertions.assertEquals(0.0, properDate.durationFrom(refDate), 2.132e-13);
    }

    @Test
    void testMedian() {
        final AbsoluteDate date1 = new AbsoluteDate(2003, 6, 13, 14, 15,
                                                    new TimeOffset(53, TimeOffset.SECOND, 12, TimeOffset.ATTOSECOND),
                                                    TimeScalesFactory.getTT());
        final AbsoluteDate date2 = new AbsoluteDate(2003, 6, 13, 14, 17,
                                                    new TimeOffset(25, TimeOffset.SECOND, 120, TimeOffset.ATTOSECOND),
                                                    TimeScalesFactory.getTT());
        final AbsoluteDate dateM = new AbsoluteDate(2003, 6, 13, 14, 16,
                                                 new TimeOffset(39, TimeOffset.SECOND, 66, TimeOffset.ATTOSECOND),
                                                 TimeScalesFactory.getTT());
        Assertions.assertEquals(dateM, AbsoluteDate.createMedian(date1, date2));
        Assertions.assertEquals(dateM, AbsoluteDate.createMedian(date2, date1));
    }

    @Test
    void testMedianInfinite() {
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY,
                                AbsoluteDate.createMedian(AbsoluteDate.FUTURE_INFINITY,
                                                          AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(AbsoluteDate.PAST_INFINITY,
                                AbsoluteDate.createMedian(AbsoluteDate.PAST_INFINITY,
                                                          AbsoluteDate.ARBITRARY_EPOCH));
    }

    @Test
    void testOffsets() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate leapStartUTC = new AbsoluteDate(1976, 12, 31, 23, 59, 59, utc);
        AbsoluteDate leapEndUTC   = new AbsoluteDate(1977,  1,  1,  0,  0,  0, utc);
        AbsoluteDate leapStartTAI = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        AbsoluteDate leapEndTAI   = new AbsoluteDate(1977,  1,  1,  0,  0, 16, tai);
        Assertions.assertEquals(leapStartUTC, leapStartTAI);
        Assertions.assertEquals(leapEndUTC, leapEndTAI);
        Assertions.assertEquals(1, leapEndUTC.offsetFrom(leapStartUTC, utc), 1.0e-10);
        Assertions.assertEquals(1, leapEndTAI.offsetFrom(leapStartTAI, utc), 1.0e-10);
        Assertions.assertEquals(2, leapEndUTC.offsetFrom(leapStartUTC, tai), 1.0e-10);
        Assertions.assertEquals(2, leapEndTAI.offsetFrom(leapStartTAI, tai), 1.0e-10);
        Assertions.assertEquals(2, leapEndUTC.durationFrom(leapStartUTC),    1.0e-10);
        Assertions.assertEquals(2, leapEndTAI.durationFrom(leapStartTAI),    1.0e-10);
    }

    @Test
    void testBeforeAndAfterLeap() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate leapStart = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        AbsoluteDate leapEnd   = new AbsoluteDate(1977,  1,  1,  0,  0, 16, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            AbsoluteDate d1 = leapStart.shiftedBy(dt);
            AbsoluteDate d2 = new AbsoluteDate(leapStart, dt, tai);
            AbsoluteDate d3 = new AbsoluteDate(leapStart, dt, utc);
            AbsoluteDate d4 = new AbsoluteDate(leapEnd,   dt, tai);
            AbsoluteDate d5 = new AbsoluteDate(leapEnd,   dt, utc);
            Assertions.assertTrue(FastMath.abs(d1.durationFrom(d2)) < 1.0e-10);
            if (dt < 0) {
                Assertions.assertTrue(FastMath.abs(d2.durationFrom(d3)) < 1.0e-10);
                Assertions.assertTrue(d4.durationFrom(d5) > (1.0 - 1.0e-10));
            } else {
                Assertions.assertTrue(d2.durationFrom(d3) < (-1.0 + 1.0e-10));
                Assertions.assertTrue(FastMath.abs(d4.durationFrom(d5)) < 1.0e-10);
            }
        }
    }

    @Test
    void testSymmetry() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate leapStart = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            Assertions.assertEquals(dt, new AbsoluteDate(leapStart, dt, utc).offsetFrom(leapStart, utc), 1.0e-10);
            Assertions.assertEquals(dt, new AbsoluteDate(leapStart, dt, tai).offsetFrom(leapStart, tai), 1.0e-10);
            Assertions.assertEquals(dt, leapStart.shiftedBy(dt).durationFrom(leapStart), 1.0e-10);
        }
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    void testEquals() {
        AbsoluteDate d1 =
            new AbsoluteDate(new DateComponents(2006, 2, 25),
                             new TimeComponents(17, 10, 34),
                             utc);
        AbsoluteDate d2 = new AbsoluteDate(new DateComponents(2006, 2, 25),
                                           new TimeComponents(17, 10, 0),
                                           utc).shiftedBy(34);
        Assertions.assertEquals(d1, d2);
        Assertions.assertNotEquals(d1, this);
    }

    @Test
    void testComponents() {
        // this is NOT J2000.0,
        // it is either a few seconds before or after depending on time scale
        DateComponents date = new DateComponents(2000, 1, 1);
        TimeComponents time = new TimeComponents(11, 59, 10);
        TimeScale[] scales = {
            TimeScalesFactory.getTAI(), TimeScalesFactory.getUTC(),
            TimeScalesFactory.getTT(), TimeScalesFactory.getTCG()
        };
        for (int i = 0; i < scales.length; ++i) {
            AbsoluteDate in = new AbsoluteDate(date, time, scales[i]);
            for (int j = 0; j < scales.length; ++j) {
                DateTimeComponents pair = in.getComponents(scales[j]);
                if (i == j) {
                    Assertions.assertEquals(date, pair.getDate());
                    Assertions.assertEquals(time, pair.getTime());
                } else {
                    Assertions.assertNotSame(date, pair.getDate());
                    Assertions.assertNotSame(time, pair.getTime());
                }
            }
        }
    }

    @Test
    void testMonth() {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assertions.assertEquals(new AbsoluteDate(2011, 2, 23, utc),
                            new AbsoluteDate(2011, Month.FEBRUARY, 23, utc));
        Assertions.assertEquals(new AbsoluteDate(2011, 2, 23, 1, 2, 3.4, utc),
                            new AbsoluteDate(2011, Month.FEBRUARY, 23, 1, 2, 3.4, utc));
    }

    @Test
    void testCCSDSUnsegmentedNoExtension() {

        AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789", utc);
        double lsb = FastMath.pow(2.0, -24);

        byte[] timeCCSDSEpoch = new byte[] { 0x53, 0x7F, 0x40, -0x70, -0x37, -0x05, -0x19 };
        for (int preamble = 0x00; preamble < 0x80; ++preamble) {
            if (preamble == 0x1F) {
                // using CCSDS reference epoch
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x04, 0x7E, -0x0B, -0x10, -0x07, 0x16, -0x79 };
        try {
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) 0x2F, (byte) 0x0, timeJ2000Epoch, null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        AbsoluteDate ccsds3 =
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) 0x2F, (byte) 0x0, timeJ2000Epoch, AbsoluteDate.J2000_EPOCH);
        Assertions.assertEquals(0, ccsds3.durationFrom(reference), lsb / 2);

    }

    @Test
    void testCCSDSUnsegmentedWithExtendedPreamble() {

        AbsoluteDate reference = new AbsoluteDate("2095-03-03T22:02:45.789012345678901", utc);
        int leap = (int) FastMath.rint(utc.offsetFromTAI(reference).toDouble());

        byte extendedPreamble = (byte) -0x80;
        byte identification   = (byte)  0x10;
        byte coarseLength1    = (byte)  0x0C; // four (3 + 1) bytes
        byte fineLength1      = (byte)  0x03; // 3 bytes
        byte coarseLength2    = (byte)  0x20; // 1 additional byte for coarse time
        byte fineLength2      = (byte)  0x10; // 4 additional bytes for fine time
        byte[] timeCCSDSEpoch = new byte[] {
             0x01,  0x02,  0x03,  0x04,  (byte)(0x05 - leap), // 5 bytes for coarse time (seconds)
            -0x37, -0x04, -0x4A, -0x74, -0x2C, -0x3C, -0x48   // 7 bytes for fine time (sub-seconds)
        };
        byte preamble1 = (byte) (extendedPreamble | identification | coarseLength1 | fineLength1);
        byte preamble2 = (byte) (coarseLength2 | fineLength2);
        AbsoluteDate ccsds1 =
                AbsoluteDate.parseCCSDSUnsegmentedTimeCode(preamble1, preamble2, timeCCSDSEpoch, null);

        // The 8 attoseconds difference comes from the fact unsegmented time is
        // in powers of 1/256 s, so it is not a whole number of attoseconds
        Assertions.assertEquals(-8.0e-18, ccsds1.durationFrom(reference), 1.0e-18);

    }

    @Test
    void testCCSDSDaySegmented() {

        AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        double lsb = 1.0e-13;
        byte[] timeCCSDSEpoch = new byte[] { 0x3F, 0x55, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };

        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x42) {
                // using CCSDS reference epoch
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) preamble, timeCCSDSEpoch, null);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) preamble, timeCCSDSEpoch, null);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };
        try {
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x4A, timeJ2000Epoch, null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        AbsoluteDate ccsds3 =
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x4A, timeJ2000Epoch, DateComponents.J2000_EPOCH);
        Assertions.assertEquals(0, ccsds3.durationFrom(reference), lsb / 2);

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, 0x0C };
        AbsoluteDate ccsds4 =
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x49, timeMicrosecond, DateComponents.J2000_EPOCH);
        Assertions.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference), lsb / 2);

    }

    @Test
    void testCCSDSCalendarSegmented() {

        AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        double lsb = 1.0e-13;

        // month of year / day of month variation
        byte[] timeMonthDay = new byte[] { 0x07, -0x2E, 0x05, 0x17, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x56) {
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies day of year variation
                    // since there is no day 1303 (= 5 * 256 + 23) in any year ...
                    Assertions.assertEquals(preamble & 0x08, 0x08);
                }

            }
        }

        // day of year variation
        byte[] timeDay = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x5E) {
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies month of year / day of month variation
                    // since there is no month 0 in any year ...
                    Assertions.assertEquals(preamble & 0x08, 0x00);
                }

            }
        }

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C };
        AbsoluteDate ccsds4 =
            AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) 0x5B, timeMicrosecond);
        Assertions.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference), lsb / 2);

    }

    @Test
    void testExpandedConstructors() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Assertions.assertEquals(new AbsoluteDate(new DateComponents(2002, 5, 28),
                            new TimeComponents(15, 30, 0),
                            TimeScalesFactory.getUTC()),
                    new AbsoluteDate(2002, 5, 28, 15, 30, 0, TimeScalesFactory.getUTC()));
            Assertions.assertEquals(new AbsoluteDate(new DateComponents(2002, 5, 28), TimeComponents.H00,
                            TimeScalesFactory.getUTC()),
                    new AbsoluteDate(2002, 5, 28, TimeScalesFactory.getUTC()));
            new AbsoluteDate(2002, 5, 28, 25, 30, 0, TimeScalesFactory.getUTC());
        });
    }

    @Test
    void testHashcode() {
        AbsoluteDate d1 =
            new AbsoluteDate(new DateComponents(2006, 2, 25),
                             new TimeComponents(17, 10, 34),
                             utc);
        AbsoluteDate d2 = new AbsoluteDate(new DateComponents(2006, 2, 25),
                                           new TimeComponents(17, 10, 0),
                                           utc).shiftedBy(34);
        Assertions.assertEquals(d1.hashCode(), d2.hashCode());
        Assertions.assertTrue(d1.hashCode() != d1.shiftedBy(1.0e-3).hashCode());
    }

    @Test
    void testInfinity() {
        Assertions.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assertions.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assertions.assertTrue(AbsoluteDate.J2000_EPOCH.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assertions.assertTrue(AbsoluteDate.J2000_EPOCH.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assertions.assertEquals(0, AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.PAST_INFINITY));
        Assertions.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) < 0);
        Assertions.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) < 0);
        Assertions.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assertions.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        Assertions.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) > 0);
        Assertions.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assertions.assertEquals(0, AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY));
        Assertions.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        Assertions.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        Assertions.assertTrue(Double.isInfinite(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        Assertions.assertTrue(Double.isNaN(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.FUTURE_INFINITY)));
        Assertions.assertTrue(Double.isNaN(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        Assertions.assertEquals("5881610-07-11T23:59:59.999Z",  AbsoluteDate.FUTURE_INFINITY.toString());
        Assertions.assertEquals("-5877490-03-03T00:00:00.000Z", AbsoluteDate.PAST_INFINITY.toString());
        Assertions.assertTrue(AbsoluteDate.FUTURE_INFINITY.equals(AbsoluteDate.FUTURE_INFINITY));
        Assertions.assertTrue(AbsoluteDate.PAST_INFINITY.equals(AbsoluteDate.PAST_INFINITY));
        Assertions.assertFalse(AbsoluteDate.PAST_INFINITY.equals(AbsoluteDate.FUTURE_INFINITY));
        Assertions.assertFalse(AbsoluteDate.FUTURE_INFINITY.equals(AbsoluteDate.PAST_INFINITY));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, AbsoluteDate.J2000_EPOCH.durationFrom(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(Double.NEGATIVE_INFINITY)));
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, AbsoluteDate.J2000_EPOCH.durationFrom(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(Double.POSITIVE_INFINITY)));

    }

    @Test
    void testCompareTo() {
        // check long time spans
        AbsoluteDate epoch =
                new AbsoluteDate(2000, 1, 1, 12, 0, 0, TimeScalesFactory.getTAI());
        Assertions.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(epoch) < 0);
        Assertions.assertTrue(epoch.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        // check short time spans
        AbsoluteDate d = epoch;
        double epsilon = 1.0 - FastMath.nextDown(1.0);
        Assertions.assertTrue(d.compareTo(d.shiftedBy(epsilon)) < 0);
        Assertions.assertEquals(0, d.compareTo(d.shiftedBy(0)));
        Assertions.assertTrue(d.compareTo(d.shiftedBy(-epsilon)) > 0);
        // check date with negative offset
        d = epoch.shiftedBy(496891466)
                .shiftedBy(0.7320114066633323)
                .shiftedBy(-19730.732011406664);
        // offset is 0 in d1
        AbsoluteDate d1 = epoch.shiftedBy(496891466 - 19730);
        Assertions.assertTrue(d.compareTo(d1) < 0);
        // decrement epoch, now offset is 0.999... in d1
        d1 = d1.shiftedBy(-1e-16);
        Assertions.assertTrue(d.compareTo(d1) < 0,"" + d.durationFrom(d1));
        // check large dates
        // these tests fail due to long overflow in durationFrom() Bug #584
        // d = new AbsoluteDate(epoch, Long.MAX_VALUE);
        // Assertions.assertEquals(-1, epoch.compareTo(d));
        // Assertions.assertTrue(d.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        // d = new AbsoluteDate(epoch, Long.MIN_VALUE);
        // Assertions.assertTrue(epoch.compareTo(d) > 0);
        // Assertions.assertTrue(d.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
    }

    @Test
    void testIsEqualTo() {
        Assertions.assertTrue(present.isEqualTo(present));
        Assertions.assertTrue(present.isEqualTo(presentToo));
        Assertions.assertFalse(present.isEqualTo(past));
        Assertions.assertFalse(present.isEqualTo(future));
    }

    @Test
    void testIsCloseTo() {
        double tolerance = 10;
        TimeStamped closeToPresent = new AnyTimeStamped(present.shiftedBy(5));
        Assertions.assertTrue(present.isCloseTo(present, tolerance));
        Assertions.assertTrue(present.isCloseTo(presentToo, tolerance));
        Assertions.assertTrue(present.isCloseTo(closeToPresent, tolerance));
        Assertions.assertFalse(present.isCloseTo(past, tolerance));
        Assertions.assertFalse(present.isCloseTo(future, tolerance));
    }

    @Test
    void testIsBefore() {
        Assertions.assertFalse(present.isBefore(past));
        Assertions.assertFalse(present.isBefore(present));
        Assertions.assertFalse(present.isBefore(presentToo));
        Assertions.assertTrue(present.isBefore(future));
    }

    @Test
    void testIsAfter() {
        Assertions.assertTrue(present.isAfter(past));
        Assertions.assertFalse(present.isAfter(present));
        Assertions.assertFalse(present.isAfter(presentToo));
        Assertions.assertFalse(present.isAfter(future));
    }

    @Test
    void testIsBeforeOrEqualTo() {
        Assertions.assertFalse(present.isBeforeOrEqualTo(past));
        Assertions.assertTrue(present.isBeforeOrEqualTo(present));
        Assertions.assertTrue(present.isBeforeOrEqualTo(presentToo));
        Assertions.assertTrue(present.isBeforeOrEqualTo(future));
    }

    @Test
    void testIsAfterOrEqualTo() {
        Assertions.assertTrue(present.isAfterOrEqualTo(past));
        Assertions.assertTrue(present.isAfterOrEqualTo(present));
        Assertions.assertTrue(present.isAfterOrEqualTo(presentToo));
        Assertions.assertFalse(present.isAfterOrEqualTo(future));
    }

    @Test
    void testIsBetween() {
        Assertions.assertTrue(present.isBetween(past, future));
        Assertions.assertTrue(present.isBetween(future, past));
        Assertions.assertFalse(past.getDate().isBetween(present, future));
        Assertions.assertFalse(past.getDate().isBetween(future, present));
        Assertions.assertFalse(future.getDate().isBetween(past, present));
        Assertions.assertFalse(future.getDate().isBetween(present, past));
        Assertions.assertFalse(present.isBetween(present, future));
        Assertions.assertFalse(present.isBetween(past, present));
        Assertions.assertFalse(present.isBetween(past, past));
        Assertions.assertFalse(present.isBetween(present, present));
        Assertions.assertFalse(present.isBetween(present, presentToo));
    }

    @Test
    void testIsBetweenOrEqualTo() {
        Assertions.assertTrue(present.isBetweenOrEqualTo(past, future));
        Assertions.assertTrue(present.isBetweenOrEqualTo(future, past));
        Assertions.assertFalse(past.getDate().isBetweenOrEqualTo(present, future));
        Assertions.assertFalse(past.getDate().isBetweenOrEqualTo(future, present));
        Assertions.assertFalse(future.getDate().isBetweenOrEqualTo(past, present));
        Assertions.assertFalse(future.getDate().isBetweenOrEqualTo(present, past));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, future));
        Assertions.assertTrue(present.isBetweenOrEqualTo(past, present));
        Assertions.assertFalse(present.isBetweenOrEqualTo(past, past));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, present));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, presentToo));
    }

    @Test
    void testAccuracy() {
        TimeScale tai = TimeScalesFactory.getTAI();
        double sec = 0.281;
        AbsoluteDate t = new AbsoluteDate(2010, 6, 21, 18, 42, sec, tai);
        double recomputedSec = t.getComponents(tai).getTime().getSecond();
        Assertions.assertEquals(sec, recomputedSec, FastMath.ulp(sec));
    }

    @Test
    void testShiftPastInfinity() {
        AbsoluteDate shifted = AbsoluteDate.PAST_INFINITY.shiftedBy(Constants.JULIAN_DAY);
        Assertions.assertEquals(AbsoluteDate.PAST_INFINITY.getSeconds(), shifted.getSeconds());
        Assertions.assertEquals(AbsoluteDate.PAST_INFINITY.getAttoSeconds(), shifted.getAttoSeconds());
    }

    @Test
    void testShiftFutureInfinity() {
        AbsoluteDate shifted = AbsoluteDate.FUTURE_INFINITY.shiftedBy(Constants.JULIAN_DAY);
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY.getSeconds(), shifted.getSeconds());
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY.getAttoSeconds(), shifted.getAttoSeconds());
    }

    @Test
    void testSubFemtoSecondPositiveShift() {
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate since = new AbsoluteDate(2008, 4, 7, 0, 53, 0.0078125, tai);
        double deltaT = 1.0e-17;
        AbsoluteDate shifted = since.shiftedBy(deltaT);
        Assertions.assertEquals(deltaT, shifted.durationFrom(since), 1.0e-25);
    }

    @Test
    void testSubFemtoSecondNegativeShift() {
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate since = new AbsoluteDate(2008, 4, 7, 0, 53, 0.0078125, tai);
        double deltaT = -1.0e-17;
        AbsoluteDate shifted = since.shiftedBy(deltaT);
        Assertions.assertEquals(deltaT, shifted.durationFrom(since), 1.0e-25);
    }

    @Test
    void testIterationAccuracy() {

        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate t0 = new AbsoluteDate(2010, 6, 21, 18, 42, 0.281, tai);

        // 0.1 is not representable exactly in double precision
        // we will accumulate error, between -0.5ULP and -3ULP at each iteration
        checkIteration(0.1, t0, 10000, 3.0, -0.3874, 1.0e-4);

        // 0.125 is representable exactly in double precision
        // error will be null
        checkIteration(0.125, t0, 10000, 1.0e-15, 0.0, 1.0e-15);

    }

    private void checkIteration(final double step, final AbsoluteDate t0, final int nMax,
                                final double maxErrorFactor,
                                final double expectedMean, final double meanTolerance) {
        final double epsilon = FastMath.ulp(step);
        AbsoluteDate iteratedDate = t0;
        double mean = 0;
        for (int i = 1; i < nMax; ++i) {
            iteratedDate = iteratedDate.shiftedBy(step);
            AbsoluteDate directDate = t0.shiftedBy(i * step);
            final double error = iteratedDate.durationFrom(directDate);
            mean += error / (i * epsilon);
            Assertions.assertEquals(0.0, iteratedDate.durationFrom(directDate), maxErrorFactor * i * epsilon);
        }
        mean /= nMax;
        Assertions.assertEquals(expectedMean, mean, meanTolerance);
    }

    @Test
    void testIssue142() {

        final AbsoluteDate epoch = AbsoluteDate.JAVA_EPOCH;
        final TimeScale utc = TimeScalesFactory.getUTC();

        Assertions.assertEquals("1970-01-01T00:00:00.000", epoch.toString(utc));
        Assertions.assertEquals(0.0, epoch.durationFrom(new AbsoluteDate(1970, 1, 1, utc)), 1.0e-15);
        Assertions.assertEquals(8.000082,
                            epoch.durationFrom(new AbsoluteDate(DateComponents.JAVA_EPOCH, TimeScalesFactory.getTAI())),
                            1.0e-15);

        // April 1, 2006, in UTC
        final TimeOffset offset = new TimeOffset(1143849600L, 0L);
        final AbsoluteDate ad = new AbsoluteDate(epoch, offset, TimeScalesFactory.getUTC());
        Assertions.assertEquals("2006-04-01T00:00:00.000", ad.toString(utc));

    }

    @Test
    void testIssue148() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t0 = new AbsoluteDate(2012, 6, 30, 23, 59, 50.0, utc);
        DateTimeComponents components = t0.shiftedBy(11.0 - 200 * Precision.EPSILON).getComponents(utc);
        Assertions.assertEquals(2012, components.getDate().getYear());
        Assertions.assertEquals(   6, components.getDate().getMonth());
        Assertions.assertEquals(  30, components.getDate().getDay());
        Assertions.assertEquals(  23, components.getTime().getHour());
        Assertions.assertEquals(  59, components.getTime().getMinute());
        Assertions.assertEquals(  61 - 200 * Precision.EPSILON,
                            components.getTime().getSecond(), 1.0e-15);
    }

    @Test
    void testIssue149() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t0 = new AbsoluteDate(2012, 6, 30, 23, 59, 59, utc);
        DateTimeComponents components = t0.shiftedBy(1.0 - Precision.EPSILON).getComponents(utc);
        Assertions.assertEquals(2012, components.getDate().getYear());
        Assertions.assertEquals(   6, components.getDate().getMonth());
        Assertions.assertEquals(  30, components.getDate().getDay());
        Assertions.assertEquals(  23, components.getTime().getHour());
        Assertions.assertEquals(  59, components.getTime().getMinute());
        Assertions.assertEquals(  60 - Precision.EPSILON,  // misleading as 60.0 - eps = 60.0
                            components.getTime().getSecond(), 1.0e-15);
    }

    @Test
    void testWrapAtMinuteEnd() {
        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date0 = new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, tai);
        AbsoluteDate ref = date0.shiftedBy(new TimeOffset(496891466L, 732011406663332300L));
        AbsoluteDate date = ref.shiftedBy(new TimeOffset(597L, 900970042626200000L).negate().multiply(33));
        DateTimeComponents dtc = date.getComponents(utc);
        Assertions.assertEquals(                2015,  dtc.getDate().getYear());
        Assertions.assertEquals(                   9,  dtc.getDate().getMonth());
        Assertions.assertEquals(                  30,  dtc.getDate().getDay());
        Assertions.assertEquals(                   7,  dtc.getTime().getHour());
        Assertions.assertEquals(                  54,  dtc.getTime().getMinute());
        Assertions.assertEquals(                  59L, dtc.getTime().getSplitSecond().getSeconds());
        Assertions.assertEquals(  999999999998732300L, dtc.getTime().getSplitSecond().getAttoSeconds());
        Assertions.assertEquals("2015-09-30T07:54:59.9999999999987323", date.toString(utc));
        AbsoluteDate beforeMidnight = new AbsoluteDate(2008, 2, 29, 23, 59, new TimeOffset(59L, 999400000000000000L), utc);
        AbsoluteDate stillBeforeMidnight = beforeMidnight.shiftedBy(new TimeOffset(0L, 200000000000000L));
        Assertions.assertEquals(59.9994, beforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assertions.assertEquals(59.9996, stillBeforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assertions.assertEquals("2008-02-29T23:59:59.9994", beforeMidnight.toString(utc));
        Assertions.assertEquals("2008-02-29T23:59:59.9996", stillBeforeMidnight.toString(utc));
    }


    @Test
    void testLastLeapOutput() {
        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t = utc.getLastKnownLeapSecond();
        Assertions.assertEquals("23:59:59.500", t.shiftedBy(-0.5).toString(utc).substring(11));
        Assertions.assertEquals("23:59:60.000", t.shiftedBy( 0.0).toString(utc).substring(11));
        Assertions.assertEquals("23:59:60.500", t.shiftedBy(+0.5).toString(utc).substring(11));
    }

    @Test
    void testWrapBeforeLeap() {
        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t = new AbsoluteDate("2015-06-30T23:59:59.999999", utc);
        Assertions.assertEquals(2015,        t.getComponents(utc).getDate().getYear());
        Assertions.assertEquals(   6,        t.getComponents(utc).getDate().getMonth());
        Assertions.assertEquals(  30,        t.getComponents(utc).getDate().getDay());
        Assertions.assertEquals(  23,        t.getComponents(utc).getTime().getHour());
        Assertions.assertEquals(  59,        t.getComponents(utc).getTime().getMinute());
        Assertions.assertEquals(  59.999999, t.getComponents(utc).getTime().getSecond(), 1.0e-6);
        Assertions.assertEquals("2015-06-30T23:59:59.999999", t.toStringWithoutUtcOffset(utc, 6));
        Assertions.assertEquals("2015-07-01T02:59:59.999999", t.toStringWithoutUtcOffset(TimeScalesFactory.getGLONASS(), 6));
    }

    @Test
    void testMjdInLeap() {
        // inside a leap second
        AbsoluteDate date1 = new AbsoluteDate(2008, 12, 31, 23, 59, 60.5, utc);

        // check date to MJD conversion
        DateTimeComponents date1Components = date1.getComponents(utc);
        int mjd = date1Components.getDate().getMJD();
        double seconds = date1Components.getTime().getSecondsInUTCDay();
        Assertions.assertEquals(54831, mjd);
        Assertions.assertEquals(86400.5, seconds, 0);

        // check MJD to date conversion
        AbsoluteDate date2 = AbsoluteDate.createMJDDate(mjd, seconds, utc);
        Assertions.assertEquals(date1, date2);

        // check we still detect seconds overflow
        try {
            AbsoluteDate.createMJDDate(mjd, seconds + 1.0, utc);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL, oiae.getSpecifier());
            Assertions.assertEquals(86401.5, (Double) oiae.getParts()[0], 0);
            Assertions.assertEquals(0, ((Number) oiae.getParts()[1]).doubleValue(), 0);
            Assertions.assertEquals(86401, ((Number) oiae.getParts()[2]).doubleValue(), 0);
        }

    }

    @Test
    void testIssueTimesStampAccuracy() {
        String testString = "2019-02-01T13:06:03.115";
        TimeScale timeScale=TimeScalesFactory.getUTC();

        DateTimeComponents expectedComponent = DateTimeComponents.parseDateTime(testString);
        AbsoluteDate expectedDate = new AbsoluteDate(expectedComponent, timeScale);

        ZonedDateTime actualComponent = LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(testString)).atZone(ZoneOffset.UTC);
        AbsoluteDate actualDate = new AbsoluteDate(Timestamp.from(actualComponent.toInstant()), timeScale);
        Assertions.assertEquals(0.0, expectedDate.durationFrom(actualDate), 1.0e-15);

    }

    @Test
    void testGetComponentsIssue681and676and694() {
        // setup
        AbsoluteDate date = new AbsoluteDate(2009, 1, 1, utc);
        double attoSecond = 1.0e-18;
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        double sixtyUlp = FastMath.ulp(60.0);
        double one = FastMath.nextDown(1.0);
        double sixty = FastMath.nextDown(60.0);
        double sixtyOne = FastMath.nextDown(61.0);

        // actions + verify
        // translate back to AbsoluteDate has up to half an ULP of error,
        // except when truncated when the error can be up to 1 ULP.
        check(date, 2009, 1, 1, 0, 0, 0, 1, 0, 0);
        check(date.shiftedBy(attoSecond), 2009, 1, 1, 0, 0, attoSecond, 0.5, 0, 0);
        check(date.shiftedBy(one), 2009, 1, 1, 0, 0, one, 0.5, 0, 0);
        // I could also see rounding to a valid time as being reasonable here
        check(date.shiftedBy(59).shiftedBy(one), 2009, 1, 1, 0, 0, sixty, 1, 1, 0);
        check(date.shiftedBy(86399).shiftedBy(one), 2009, 1, 1, 23, 59, sixty, 1, 1, 0);
        check(date.shiftedBy(-zeroUlp), 2009, 1, 1, 0, 0, 0, 0.5, 0, 0);
        check(date.shiftedBy(-oneUlp), 2008, 12, 31, 23, 59, sixtyOne, 1, 1, 0);
        check(date.shiftedBy(-1).shiftedBy(zeroUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-zeroUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-oneUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-sixtyUlp), 2008, 12, 31, 23, 59, sixty, 0.5, 0, 0);
        check(date.shiftedBy(-61).shiftedBy(attoSecond), 2008, 12, 31, 23, 59, attoSecond, 0.5, 0, 0);

        // check UTC weirdness.
        // These have more error because of additional multiplications and additions
        // up to 2 ULPs or ulp(60.0) of error.
        AbsoluteDate d = new AbsoluteDate(1966, 1, 1, utc);
        double ratePost = 0.0025920 / Constants.JULIAN_DAY;
        double factorPost = ratePost / (1 + ratePost);
        double ratePre = 0.0012960 / Constants.JULIAN_DAY;
        double factorPre = ratePre / (1 + ratePre);
        check(d, 1966, 1, 1, 0, 0, 0, 1, 0, 0);
        check(d.shiftedBy(zeroUlp), 1966, 1, 1, 0, 0, 0, 0.5, 0, 0);
        check(d.shiftedBy(attoSecond), 1966, 1, 1, 0, 0, attoSecond, 0.5, 0, 0);
        check(d.shiftedBy(one), 1966, 1, 1, 0, 0, one * (1 - factorPost), 1, 3, 0);
        check(d.shiftedBy(59).shiftedBy(one), 1966, 1, 1, 0, 0, sixty * (1 - factorPost), 1, 2, 0);
        check(d.shiftedBy(86399).shiftedBy(one), 1966, 1, 1, 23, 59, sixty - 86400 * factorPost, 1, 1, 0);
        check(d.shiftedBy(-zeroUlp), 1966, 1, 1, 0, 0, 0, 0.5, 0, 0);
        // actual leap is small ~1e-16, but during a leap rounding up to 60.0 is ok
        check(d.shiftedBy(-oneUlp), 1965, 12, 31, 23, 59, 60.0, 1, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(zeroUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-zeroUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-oneUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-sixtyUlp), 1965, 12, 31, 23, 59, 59 + (1 + sixtyUlp) * factorPre, 0.5, 1, 0);
        // since second ~= 0 there is significant cancellation
        check(d.shiftedBy(-60).shiftedBy(zeroUlp), 1965, 12, 31, 23, 59, 60 * factorPre, 0, 0, sixtyUlp);
        check(d.shiftedBy(-60).shiftedBy(oneUlp), 1965, 12, 31, 23, 59, (oneUlp - oneUlp * factorPre) + 60 * factorPre, 0.5, 0, sixtyUlp);

        // check first whole second leap
        AbsoluteDate d2 = new AbsoluteDate(1972, 7, 1, utc);
        check(d2, 1972, 7, 1, 0, 0, 0, 1, 0, 0);
        check(d2.shiftedBy(attoSecond), 1972, 7, 1, 0, 0, attoSecond, 0.5, 0, 0);
        check(d2.shiftedBy(one), 1972, 7, 1, 0, 0, one, 0.5, 0, 0);
        check(d2.shiftedBy(59).shiftedBy(one), 1972, 7, 1, 0, 0, sixty, 1, 1, 0);
        check(d2.shiftedBy(86399).shiftedBy(one), 1972, 7, 1, 23, 59, sixty, 1, 1, 0);
        check(d2.shiftedBy(-zeroUlp), 1972, 7, 1, 0, 0, 0, 0.5, 0, 0);
        check(d2.shiftedBy(-oneUlp), 1972, 6, 30, 23, 59, sixtyOne, 1, 1, 0);
        check(d2.shiftedBy(-1).shiftedBy(zeroUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-zeroUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-oneUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-sixtyUlp), 1972, 6, 30, 23, 59, sixty, 0.5, 0, 0);
        check(d2.shiftedBy(-61).shiftedBy(attoSecond), 1972, 6, 30, 23, 59, attoSecond, 0.5, 0, 0);

        // check first leap second, which was actually 1.422818 s.
        AbsoluteDate d3 = AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1230724800);
        check(d3, 1960, 12, 31, 23, 59, 60, 0.5, 0, 0);
        AbsoluteDate d4 = new AbsoluteDate(1961, 1, 1, utc);
        check(d4, 1961, 1, 1, 0, 0, 0, 0.5, 0, 0);
        // FIXME something wrong because a date a smidgen before 1961-01-01 is not in a leap second
        //check(d4.shiftedBy(-oneUlp), 1960, 12, 31, 23, 59, 61.422818, 0.5, 0, 0);

        // check NaN, this is weird that NaNs have valid ymdhm, but not second.
        DateTimeComponents actual = date.shiftedBy(Double.NaN).getComponents(utc);
        DateComponents dc = actual.getDate();
        TimeComponents tc = actual.getTime();
        MatcherAssert.assertThat(dc.getYear(), CoreMatchers.is(2000));
        MatcherAssert.assertThat(dc.getMonth(), CoreMatchers.is(1));
        MatcherAssert.assertThat(dc.getDay(), CoreMatchers.is(1));
        MatcherAssert.assertThat(tc.getHour(), CoreMatchers.is(0));
        MatcherAssert.assertThat(tc.getMinute(), CoreMatchers.is(0));
        MatcherAssert.assertThat("second", tc.getSecond(), CoreMatchers.is(Double.NaN));
        MatcherAssert.assertThat(tc.getMinutesFromUTC(), CoreMatchers.is(0));
        final double difference = new AbsoluteDate(actual, utc).durationFrom(date);
        MatcherAssert.assertThat(difference, CoreMatchers.is(Double.NaN));
    }

    private void check(AbsoluteDate date,
                       int year, int month, int day, int hour, int minute, double second,
                       double roundTripUlps, final int secondUlps, final double absTol) {
        DateTimeComponents actual = date.getComponents(utc);
        DateComponents d = actual.getDate();
        TimeComponents t = actual.getTime();
        MatcherAssert.assertThat(d.getYear(), CoreMatchers.is(year));
        MatcherAssert.assertThat(d.getMonth(), CoreMatchers.is(month));
        MatcherAssert.assertThat(d.getDay(), CoreMatchers.is(day));
        MatcherAssert.assertThat(t.getHour(), CoreMatchers.is(hour));
        MatcherAssert.assertThat(t.getMinute(), CoreMatchers.is(minute));
        MatcherAssert.assertThat("second", t.getSecond(),
                OrekitMatchers.numberCloseTo(second, absTol, secondUlps));
        MatcherAssert.assertThat(t.getMinutesFromUTC(), CoreMatchers.is(0));
        final double tol = FastMath.ulp(second) * roundTripUlps;
        final double difference = new AbsoluteDate(actual, utc).durationFrom(date);
        MatcherAssert.assertThat(difference,
                OrekitMatchers.closeTo(0, FastMath.max(absTol, tol)));
    }

    /** Check {@link AbsoluteDate#toStringRfc3339(TimeScale)}. */
    @Test
    void testToStringRfc3339() {
        // setup
        AbsoluteDate date = new AbsoluteDate(2009, 1, 1, utc);
        double one = FastMath.nextDown(1.0);
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        //double sixty = FastMath.nextDown(60.0);
        double sixtyUlp = FastMath.ulp(60.0);

        // action
        // test midnight
        check(date, "2009-01-01T00:00:00Z");
        check(date.shiftedBy(1), "2009-01-01T00:00:01Z");
        // test digits and rounding
        check(date.shiftedBy(new TimeOffset(12L, 345678912345678900L)), "2009-01-01T00:00:12.3456789123456789Z");
        check(date.shiftedBy(new TimeOffset(0L, 12345678912345678L)), "2009-01-01T00:00:00.012345678912345678Z");
        // test min and max values
        check(date.shiftedBy(zeroUlp), "2009-01-01T00:00:00Z");
        check(date.shiftedBy(59.0).shiftedBy(one), "2009-01-01T00:00:59.999999999999999889Z");
        check(date.shiftedBy(86399).shiftedBy(one), "2009-01-01T23:59:59.999999999999999889Z");
        check(date.shiftedBy(oneUlp), "2009-01-01T00:00:00.000000000000000222Z");
        check(date.shiftedBy(one), "2009-01-01T00:00:00.999999999999999889Z");
        check(date.shiftedBy(-zeroUlp), "2009-01-01T00:00:00Z");
        // test leap
        check(date.shiftedBy(-oneUlp), "2008-12-31T23:59:60.999999999999999778Z");
        check(date.shiftedBy(-1).shiftedBy(one), "2008-12-31T23:59:60.999999999999999889Z");
        check(date.shiftedBy(-0.5), "2008-12-31T23:59:60.5Z");
        check(date.shiftedBy(-1).shiftedBy(zeroUlp), "2008-12-31T23:59:60Z");
        check(date.shiftedBy(-1), "2008-12-31T23:59:60Z");
        check(date.shiftedBy(-1).shiftedBy(-zeroUlp), "2008-12-31T23:59:60Z");
        check(date.shiftedBy(-1).shiftedBy(-oneUlp), "2008-12-31T23:59:59.999999999999999778Z");
        check(date.shiftedBy(-2), "2008-12-31T23:59:59Z");
        check(date.shiftedBy(-1).shiftedBy(-sixtyUlp), "2008-12-31T23:59:59.999999999999992895Z");
        check(date.shiftedBy(-61).shiftedBy(zeroUlp), "2008-12-31T23:59:00Z");
        check(date.shiftedBy(-61).shiftedBy(oneUlp), "2008-12-31T23:59:00.000000000000000222Z");
        // test UTC weirdness
        AbsoluteDate d = new AbsoluteDate(1966, 1, 1, utc);
        check(d, "1966-01-01T00:00:00Z");
        check(d.shiftedBy(zeroUlp), "1966-01-01T00:00:00Z");
        check(d.shiftedBy(oneUlp), "1966-01-01T00:00:00.000000000000000222Z");
        // as we are after the 1966 leap, slope is 30ns/s
        // decimals should therefore be (1 - 2⁻⁵³) ⨉ 10⁹ / (10⁹ + 30) ≈ 0.9999999700000007889776…
        // Orekit 13.0 is accurate to attosecond
        check(d.shiftedBy(one), "1966-01-01T00:00:00.999999970000000789Z");
        // as we are after the 1966 leap, slope is 30ns/s
        // decimals should therefore be [59 + (1 - 2⁻⁵³)] ⨉ 10⁹ / (10⁹ + 30) ≈ 0.9999982000000538889760…
        // Orekit 13.0 is accurate to attosecond
        check(d.shiftedBy(59).shiftedBy(one), "1966-01-01T00:00:59.999998200000053889Z");
        // as we are after the 1966 leap, slope is 30ns/s
        // decimals should therefore be [86399 + (1 - 2⁻⁵³)] ⨉ 10⁹ / (10⁹ + 30) ≈ 0.9974080000777598866449…
        // Orekit 13.0 is accurate to attosecond
        check(d.shiftedBy(86399).shiftedBy(one), "1966-01-01T23:59:59.997408000077759887Z");
        check(d.shiftedBy(-zeroUlp), "1966-01-01T00:00:00Z");
        // actual leap is small ~1e-16, Orekit 13.0 get it
        check(d.shiftedBy(-oneUlp), "1965-12-31T23:59:59.999999999999999779Z");
        // as we are before the 1966 leap, slope is 15ns/s
        // decimals should therefore be 15 / (10⁹ + 15) ≈ 0.000000014999999775000003375…
        // Orekit 13.0 is accurate to attosecond
        check(d.shiftedBy(-1).shiftedBy(zeroUlp), "1965-12-31T23:59:59.000000014999999776Z");
        check(d.shiftedBy(-1).shiftedBy(-zeroUlp), "1965-12-31T23:59:59.000000014999999776Z");
        // we subtract ulp(1) = 2⁻⁵² ≈ 222 as
        // Orekit 13.0 is accurate to attosecond
        check(d.shiftedBy(-1).shiftedBy(-oneUlp), "1965-12-31T23:59:59.000000014999999554Z");
        // we subtract ulp(60) = 2⁻⁴⁷ ≈ 7105 as
        // Orekit 13.0 is accurate to attosecond
        check(d.shiftedBy(-1).shiftedBy(-sixtyUlp), "1965-12-31T23:59:59.000000014999992671Z");
        // since second ~= 0 there is significant cancellation
        // 60 ⨉ 15 / (10⁹ + 15) ≈ 0.0000008999999865000002025
        check(d.shiftedBy(-60).shiftedBy(zeroUlp), "1965-12-31T23:59:00.000000899999986501Z");
        check(d.shiftedBy(-60).shiftedBy(oneUlp), "1965-12-31T23:59:00.000000899999986723Z");

        // check first leap second, which was actually 1.422818 s.
        check(new AbsoluteDate(1961, 1, 1, utc), "1961-01-01T00:00:00Z");
        AbsoluteDate d3 = AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1230724800);
        check(d3, "1960-12-31T23:59:60Z");
        // FIXME something wrong because a date a smidgen before 1961-01-01 is not in a leap second
        //check(d3.shiftedBy(FastMath.nextDown(1.422818)), "1960-12-31T23:59:61.422818Z");

        // test proleptic
        check(new AbsoluteDate(123, 4, 5, 6, 7, new TimeOffset(8, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND), utc),
              "0123-04-05T06:07:08.9Z");

        // there is no way to produce valid RFC3339 for these cases
        // I would rather print something useful than throw an exception
        // so these cases don't check for a correct answer, just an informative one
        check(new AbsoluteDate(-123, 4, 5, 6, 7, new TimeOffset(8, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND), utc),
              "-123-04-05T06:07:08.9Z");
        check(new AbsoluteDate(-1230, 4, 5, 6, 7, new TimeOffset(8, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND), utc),
              "-1230-04-05T06:07:08.9Z");
        // test far future
        check(new AbsoluteDate(12300, 4, 5, 6, 7, new TimeOffset(8, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND), utc),
              "12300-04-05T06:07:08.9Z");
        // test infinity
        check(AbsoluteDate.FUTURE_INFINITY, "5881610-07-11T23:59:59.999Z");
        check(AbsoluteDate.PAST_INFINITY, "-5877490-03-03T00:00:00Z");
        // test NaN
        check(date.shiftedBy(Double.NaN), "2000-01-01T00:00:NaNZ");
    }

    private void check(final AbsoluteDate d, final String s) {
        MatcherAssert.assertThat(d.toStringRfc3339(utc),
                CoreMatchers.is(s));
        MatcherAssert.assertThat(d.getComponents(utc).toStringRfc3339(),
                CoreMatchers.is(s));
    }


    /** Check {@link AbsoluteDate#toString()}. */
    @Test
    void testToString() {
        // setup
        AbsoluteDate date = new AbsoluteDate(2009, 1, 1, utc);
        double one = FastMath.nextDown(1.0);
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        //double sixty = FastMath.nextDown(60.0);
        double sixtyUlp = FastMath.ulp(60.0);

        // action
        // test midnight
        checkToString(date, "2009-01-01T00:00:00.000");
        checkToString(date.shiftedBy(1), "2009-01-01T00:00:01.000");
        // test digits and rounding
        checkToString(date.shiftedBy(new TimeOffset(12L, 345678912345678900L)), "2009-01-01T00:00:12.3456789123456789");
        checkToString(date.shiftedBy(new TimeOffset(0L, 12345678912345678L)), "2009-01-01T00:00:00.012345678912345678");
        // test min and max values
        checkToString(date.shiftedBy(zeroUlp), "2009-01-01T00:00:00.000");
        // Orekit 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(59.0).shiftedBy(one), "2009-01-01T00:00:59.999999999999999889");
        // Orekit 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(86399).shiftedBy(one), "2009-01-01T23:59:59.999999999999999889");
        // Orekit 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(oneUlp), "2009-01-01T00:00:00.000000000000000222");
        // Orekit 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(one), "2009-01-01T00:00:00.999999999999999889");
        checkToString(date.shiftedBy(-zeroUlp), "2009-01-01T00:00:00.000");
        // test leap
        // Orekit 10.1 throw OIAE, 10.2 rounds up, 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(-oneUlp), "2008-12-31T23:59:60.999999999999999778");
        // Orekit 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(-1).shiftedBy(one), "2008-12-31T23:59:60.999999999999999889");
        checkToString(date.shiftedBy(-0.5), "2008-12-31T23:59:60.500");
        checkToString(date.shiftedBy(-1).shiftedBy(zeroUlp), "2008-12-31T23:59:60.000");
        checkToString(date.shiftedBy(-1), "2008-12-31T23:59:60.000");
        checkToString(date.shiftedBy(-1).shiftedBy(-zeroUlp), "2008-12-31T23:59:60.000");
        // Orekit 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(-1).shiftedBy(-oneUlp), "2008-12-31T23:59:59.999999999999999778");
        checkToString(date.shiftedBy(-2), "2008-12-31T23:59:59.000");
        // Orekit 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(-1).shiftedBy(-sixtyUlp), "2008-12-31T23:59:59.999999999999992895");
        checkToString(date.shiftedBy(-61).shiftedBy(zeroUlp), "2008-12-31T23:59:00.000");
        // Orekit 13.0 is accurate to attosecond
        checkToString(date.shiftedBy(-61).shiftedBy(oneUlp), "2008-12-31T23:59:00.000000000000000222");
        // test UTC weirdness
        AbsoluteDate d = new AbsoluteDate(1966, 1, 1, utc);
        checkToString(d, "1966-01-01T00:00:00.000");
        checkToString(d.shiftedBy(zeroUlp), "1966-01-01T00:00:00.000");
        checkToString(d.shiftedBy(oneUlp), "1966-01-01T00:00:00.000000000000000222");
        // as we are after the 1966 leap, slope is 30ns/s
        // decimals should therefore be (1 - 2⁻⁵³) ⨉ 10⁹ / (10⁹ + 30) ≈ 0.9999999700000007889776…
        // Orekit 13.0 is accurate to attosecond
        checkToString(d.shiftedBy(one), "1966-01-01T00:00:00.999999970000000789");
        // as we are after the 1966 leap, slope is 30ns/s
        // decimals should therefore be [59 + (1 - 2⁻⁵³)] ⨉ 10⁹ / (10⁹ + 30) ≈ 0.9999982000000538889760…
        // Orekit 13.0 is accurate to attosecond
        checkToString(d.shiftedBy(59).shiftedBy(one), "1966-01-01T00:00:59.999998200000053889");
        // as we are after the 1966 leap, slope is 30ns/s
        // decimals should therefore be [86399 + (1 - 2⁻⁵³)] ⨉ 10⁹ / (10⁹ + 30) ≈ 0.9974080000777598866449…
        // Orekit 13.0 is accurate to attosecond
        checkToString(d.shiftedBy(86399).shiftedBy(one), "1966-01-01T23:59:59.997408000077759887");
        checkToString(d.shiftedBy(-zeroUlp), "1966-01-01T00:00:00.000");
        // actual leap is small ~1e-16, Orekit 13.0 get it
        checkToString(d.shiftedBy(-oneUlp), "1965-12-31T23:59:59.999999999999999779");
        // as we are before the 1966 leap, slope is 15ns/s
        // decimals should therefore be 15 / (10⁹ + 15) ≈ 0.000000014999999775000003375…
        // Orekit 13.0 is accurate to attosecond
        checkToString(d.shiftedBy(-1).shiftedBy(zeroUlp), "1965-12-31T23:59:59.000000014999999776");
        checkToString(d.shiftedBy(-1).shiftedBy(-zeroUlp), "1965-12-31T23:59:59.000000014999999776");
        // we subtract ulp(1) = 2⁻⁵² ≈ 222 as
        // Orekit 13.0 is accurate to attosecond
        checkToString(d.shiftedBy(-1).shiftedBy(-oneUlp), "1965-12-31T23:59:59.000000014999999554");
        // we subtract ulp(60) = 2⁻⁴⁷ ≈ 7105 as
        // Orekit 13.0 is accurate to attosecond
        checkToString(d.shiftedBy(-1).shiftedBy(-sixtyUlp), "1965-12-31T23:59:59.000000014999992671");
        // 60 ⨉ 15 / (10⁹ + 15) ≈ 0.0000008999999865000002025
        checkToString(d.shiftedBy(-60).shiftedBy(zeroUlp), "1965-12-31T23:59:00.000000899999986501");
        checkToString(d.shiftedBy(-60).shiftedBy(oneUlp), "1965-12-31T23:59:00.000000899999986723");

        // check first leap second, which was actually 1.422818 s.
        checkToString(new AbsoluteDate(1961, 1, 1, utc), "1961-01-01T00:00:00.000");
        AbsoluteDate d3 = AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1230724800);
        checkToString(d3, "1960-12-31T23:59:60.000");
        // FIXME something wrong because a date a smidgen before 1961-01-01 is not in a leap second
        //checkToString(d3.shiftedBy(FastMath.nextDown(1.422818)), "1960-12-31T23:59:61.423");

        // test proleptic
        checkToString(new AbsoluteDate(123, 4, 5, 6, 7, new TimeOffset(8, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND), utc),
                      "0123-04-05T06:07:08.900");

        // there is no way to produce valid RFC3339 for these cases
        // I would rather print something useful than throw an exception
        // so these cases don't check for a correct answer, just an informative one
        checkToString(new AbsoluteDate(-123, 4, 5, 6, 7, new TimeOffset(8, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND), utc),
                      "-123-04-05T06:07:08.900");
        checkToString(new AbsoluteDate(-1230, 4, 5, 6, 7, new TimeOffset(8, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND), utc),
                      "-1230-04-05T06:07:08.900");
        // test far future
        checkToString(new AbsoluteDate(12300, 4, 5, 6, 7, new TimeOffset(8, TimeOffset.SECOND, 900, TimeOffset.MILLISECOND), utc),
                      "12300-04-05T06:07:08.900");
        // test infinity
        checkToString(AbsoluteDate.FUTURE_INFINITY, "5881610-07-11T23:59:59.999");
        checkToString(AbsoluteDate.PAST_INFINITY, "-5877490-03-03T00:00:00.000");
        // test NaN
        checkToString(date.shiftedBy(Double.NaN), "2000-01-01T00:00:NaN");
    }

    private void checkToString(final AbsoluteDate d, final String s) {
        MatcherAssert.assertThat(d.toString(), CoreMatchers.is(s + "Z"));
        MatcherAssert.assertThat(d.getComponents(utc).toString(), CoreMatchers.is(s + "+00:00"));
    }

    @Test
    void testToStringWithoutUtcOffset() {
        // setup
        AbsoluteDate date = new AbsoluteDate(2009, 1, 1, utc);
        double one = FastMath.nextDown(1.0);
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        //double sixty = FastMath.nextDown(60.0);
        double sixtyUlp = FastMath.ulp(60.0);

        // action
        // test midnight
        checkToStringNoOffset(date, "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(1), "2009-01-01T00:00:01.000");
        // test digits and rounding
        checkToStringNoOffset(date.shiftedBy(12.3456789123456789), "2009-01-01T00:00:12.346");
        checkToStringNoOffset(date.shiftedBy(0.0123456789123456789), "2009-01-01T00:00:00.012");
        // test min and max values
        checkToStringNoOffset(date.shiftedBy(zeroUlp), "2009-01-01T00:00:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(59.0).shiftedBy(one), "2009-01-01T00:01:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(86399).shiftedBy(one), "2009-01-02T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(oneUlp), "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(one), "2009-01-01T00:00:01.000");
        checkToStringNoOffset(date.shiftedBy(-zeroUlp), "2009-01-01T00:00:00.000");
        // test leap
        // Orekit 10.1 throw OIAE, 10.2 rounds up
        checkToStringNoOffset(date.shiftedBy(-oneUlp), "2009-01-01T00:00:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(one), "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(-0.5), "2008-12-31T23:59:60.500");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(zeroUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-zeroUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-oneUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-2), "2008-12-31T23:59:59.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-sixtyUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-61).shiftedBy(zeroUlp), "2008-12-31T23:59:00.000");
        checkToStringNoOffset(date.shiftedBy(-61).shiftedBy(oneUlp), "2008-12-31T23:59:00.000");
    }


    private void checkToStringNoOffset(final AbsoluteDate d, final String s) {
        MatcherAssert.assertThat(d.toStringWithoutUtcOffset(utc, 3), CoreMatchers.is(s));
        MatcherAssert.assertThat(
                d.getComponents(utc).toStringWithoutUtcOffset(utc.minuteDuration(d), 3),
                CoreMatchers.is(s));
    }

    /**
     * Check {@link AbsoluteDate#toString()} when UTC throws an exception. This ~is~ was
     * the most common issue new and old users face.
     */
    @Test
    void testToStringException() {
        Utils.setDataRoot("no-data");
        try {
            DataContext.getDefault().getTimeScales().getUTC();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            // expected
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.NO_IERS_UTC_TAI_HISTORY_DATA_LOADED);
        }
        // try some unusual values
        MatcherAssert.assertThat(present.toString(), CoreMatchers.is("2000-01-01T12:00:32.000 TAI"));
        MatcherAssert.assertThat(present.shiftedBy(Double.POSITIVE_INFINITY).toString(),
                                 CoreMatchers.is("5881610-07-11T23:59:59.999 TAI"));
        MatcherAssert.assertThat(present.shiftedBy(Double.NEGATIVE_INFINITY).toString(),
                                 CoreMatchers.is("-5877490-03-03T00:00:00.000 TAI"));
        MatcherAssert.assertThat(present.shiftedBy(Double.NaN).toString(),
                                 CoreMatchers.is("2000-01-01T00:00:NaN TAI"));
        // infinity is special cased, but I can make AbsoluteDate.offset larger than
        // Long.MAX_VALUE see #584
        Assertions.assertTrue(Double.isInfinite(present.shiftedBy(1e300).durationFrom(present)));
    }

    /** Test for issue 943: management of past and future infinity in equality checks. */
    @Test
    void test_issue_943() {

        // Run issue test
        final AbsoluteDate date1 = new AbsoluteDate(AbsoluteDate.PAST_INFINITY, 0);
        final AbsoluteDate date2 = new AbsoluteDate(AbsoluteDate.PAST_INFINITY, 0);
        date1.durationFrom(date2);
        Assertions.assertEquals(date1, date2);

        // Check equality is as expected for PAST INFINITY
        final AbsoluteDate date3 = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate date4 = new AbsoluteDate(AbsoluteDate.PAST_INFINITY, 0);
        Assertions.assertEquals(date3, date4);

        // Check equality is as expected for FUTURE INFINITY
        final AbsoluteDate date5 = AbsoluteDate.FUTURE_INFINITY;
        final AbsoluteDate date6 = new AbsoluteDate(AbsoluteDate.FUTURE_INFINITY, 0);
        Assertions.assertEquals(date5, date6);

        // Check inequality is as expected
        final AbsoluteDate date7 = new AbsoluteDate(AbsoluteDate.PAST_INFINITY, 0);
        final AbsoluteDate date8 = new AbsoluteDate(AbsoluteDate.FUTURE_INFINITY, 0);
        Assertions.assertNotEquals(date7, date8);

        // Check inequality is as expected
        final AbsoluteDate date9 = new AbsoluteDate(new TimeOffset(Double.POSITIVE_INFINITY));
        final AbsoluteDate date10 = new AbsoluteDate(new TimeOffset(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(date9, date10);
    }

    @Test
    void testNegativeOffsetConstructor() {
        AbsoluteDate date = new AbsoluteDate(2019, 10, 11, 20, 40,
                                             FastMath.scalb(6629298651489277.0, -55),
                                             TimeScalesFactory.getTT());
        AbsoluteDate after = date.shiftedBy(Precision.EPSILON);
        Assertions.assertEquals(624098367L, date.getSeconds());
        Assertions.assertEquals(FastMath.nextAfter(1.0, Double.NEGATIVE_INFINITY), 1.0e-18 * date.getAttoSeconds(), 2.4e-15);
        Assertions.assertEquals(Precision.EPSILON, after.durationFrom(date), 1.0e-18);
    }

    @Test
    void testNegativeOffsetShift() {
        AbsoluteDate reference = new AbsoluteDate(2019, 10, 11, 20, 40,
                                                  1.6667019180022178E-7,
                                                  TimeScalesFactory.getTAI());
        double dt = FastMath.scalb(6596520010750484.0, -39);
        AbsoluteDate shifted = reference.shiftedBy(dt);
        AbsoluteDate after = shifted.shiftedBy(Precision.EPSILON);
        Assertions.assertEquals(624110398L, shifted.getSeconds());
        Assertions.assertEquals((1.0 - 1.6922e-13) * 1.0e18, shifted.getAttoSeconds(), 1.0e-15);
        Assertions.assertEquals(Precision.EPSILON, after.durationFrom(shifted), 1.0e-18);
    }

    @Test
    void testDurationFromWithTimeUnit() {
        AbsoluteDate reference = new AbsoluteDate(2023, 1, 1, 12, 13, 59.12334567, utc);
        for (TimeUnit timeUnit : TimeUnit.values()) {
            Assertions.assertEquals(0, reference.durationFrom(reference, timeUnit));

            long dayInTimeUnit = timeUnit.convert((long) Constants.JULIAN_DAY, TimeUnit.SECONDS);
            for (int i = 1; i <= 365; i++) {
              AbsoluteDate minusDays = reference.shiftedBy(-i * Constants.JULIAN_DAY);
              AbsoluteDate plusDays = reference.shiftedBy(i* Constants.JULIAN_DAY);


              Assertions.assertEquals(i * dayInTimeUnit, reference.durationFrom(minusDays, timeUnit));

              Assertions.assertEquals(-i * dayInTimeUnit, reference.durationFrom(plusDays, timeUnit));
            }

           for (long ns = 1; ns <= 1_000_000_000; ns += 1_000_000) {
              AbsoluteDate minus = reference.shiftedBy(-1e-9 * ns);
              AbsoluteDate plus = reference.shiftedBy(1e-9 * ns);

              double deltaInTimeUnit = ns / (double) timeUnit.toNanos(1);
              Assertions.assertEquals(FastMath.round(deltaInTimeUnit), reference.durationFrom(minus, timeUnit),
                  String.format("TimeUnit: %s, ns: %d", timeUnit, ns));

              Assertions.assertEquals(FastMath.round(-deltaInTimeUnit), reference.durationFrom(plus, timeUnit),
                  String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
            }


        }
    }

    @Test
    void testConstructWithTimeUnitOffset() {
      AbsoluteDate reference = new AbsoluteDate(2023, 1, 1, 12, 13, 59.12334567, utc);

      for (TimeUnit timeUnit : TimeUnit.values()) {
        Assertions.assertEquals(0,
            FastMath.abs(reference.durationFrom(new AbsoluteDate(reference, 0, timeUnit))), 1e-10);

        long dayInTimeUnit = timeUnit.convert((long) Constants.JULIAN_DAY, TimeUnit.SECONDS);
        for (int i = 1; i <= 365; i++) {
          AbsoluteDate minusDays = reference.shiftedBy(-i * Constants.JULIAN_DAY);
          AbsoluteDate plusDays = reference.shiftedBy(i* Constants.JULIAN_DAY);

          Assertions.assertEquals(0,
              FastMath.abs(reference.durationFrom(new AbsoluteDate(minusDays, i * dayInTimeUnit, timeUnit))),
              1e-10,
              String.format("TimeUnit: %s", timeUnit));
          Assertions.assertEquals(0,
              FastMath.abs(reference.durationFrom(new AbsoluteDate(plusDays, -i * dayInTimeUnit, timeUnit))),
              1e-10,
              String.format("TimeUnit: %s", timeUnit));
        }

        for (long ns = 1; ns <= 1_000_000_000; ns += 1_000_000) {
          if (timeUnit.convert(1, TimeUnit.SECONDS) < 1) {
            //Skip everything larger than one second
            continue;
          }
          AbsoluteDate minus = reference.shiftedBy(-1e-9 * ns);
          AbsoluteDate plus = reference.shiftedBy(1e-9 * ns);

          double deltaInTimeUnit =  ns / (double) timeUnit.toNanos(1);
          Assertions.assertEquals(0,
              FastMath.abs(reference.durationFrom(new AbsoluteDate(minus, FastMath.round(deltaInTimeUnit), timeUnit))),
              1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
              String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
          Assertions.assertEquals(0,
              FastMath.abs(reference.durationFrom(new AbsoluteDate(plus, FastMath.round(-deltaInTimeUnit), timeUnit))),
              1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
              String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
        }
      }
    }

    @Test
    void testShiftedByWithTimeUnit() {
        AbsoluteDate reference = new AbsoluteDate(2023, 1, 1, 12, 13, 59.12334567, utc);

        for (TimeUnit timeUnit : TimeUnit.values()) {
            Assertions.assertEquals(0,
                FastMath.abs(reference.durationFrom(reference.shiftedBy( 0, timeUnit))), 1e-10);

            long dayInTimeUnit = timeUnit.convert((long) Constants.JULIAN_DAY, TimeUnit.SECONDS);
            for (int i = 1; i <= 365; i++) {
                AbsoluteDate minusDays = reference.shiftedBy(-i * Constants.JULIAN_DAY);
                AbsoluteDate plusDays = reference.shiftedBy(i* Constants.JULIAN_DAY);

                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(minusDays.shiftedBy(i * dayInTimeUnit, timeUnit))),
                    1e-10,
                    String.format("TimeUnit: %s", timeUnit));
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(plusDays.shiftedBy(-i * dayInTimeUnit, timeUnit))),
                    1e-10,
                    String.format("TimeUnit: %s", timeUnit));
            }

            for (long ns = 1; ns <= 1_000_000_000; ns += 1_000_000) {
                if (timeUnit.convert(1, TimeUnit.SECONDS) < 1) {
                    //Skip everything larger than one second
                    continue;
                }
                AbsoluteDate minus = reference.shiftedBy(-1e-9 * ns);
                AbsoluteDate plus = reference.shiftedBy(1e-9 * ns);

                double deltaInTimeUnit =  ns / (double) timeUnit.toNanos(1);
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(minus.shiftedBy(FastMath.round(deltaInTimeUnit), timeUnit))),
                    1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
                Assertions.assertEquals(0,
                    FastMath.abs(reference.durationFrom(plus.shiftedBy(FastMath.round(-deltaInTimeUnit), timeUnit))),
                    1.0 / timeUnit.convert(1, TimeUnit.SECONDS),
                    String.format("TimeUnit: %s, ns: %d", timeUnit, ns));
            }
        }
    }

    @Test
    void testGetJulianDates() {

        // GIVEN a reference date
        final TimeScale utc = TimeScalesFactory.getUTC();

        AbsoluteDate reference              = new AbsoluteDate(2024, 7, 4, 13, 0, 0, utc);
        AbsoluteDate referenceFromJDMethod  = AbsoluteDate.createJDDate(2460496, .0416667 * Constants.JULIAN_DAY, utc);
        AbsoluteDate referenceFromMJDMethod = AbsoluteDate.createMJDDate(60495, 0.54166670 * Constants.JULIAN_DAY, utc);

        // WHEN converting it to Julian Date or Modified Julian Date
        double mjdDateDefaultData = reference.getMJD();
        double jdDateDefaultData  = reference.getJD();
        double mjdDate            = reference.getMJD(utc);
        double jdDate             = reference.getJD(utc);

        // THEN
        // source : Time/Date Converter - HEASARC - NASA
        Assertions.assertEquals(2460496.0416667, jdDateDefaultData, 1.0e-6);
        Assertions.assertEquals(60495.54166670, mjdDateDefaultData, 1.0e-6);
        Assertions.assertEquals(jdDate, jdDateDefaultData, 1.0e-6);
        Assertions.assertEquals(mjdDateDefaultData, mjdDate);

        // Assert that static method are correct when creating date from JD or MJD
        Assertions.assertTrue(reference.isCloseTo(referenceFromJDMethod, 1e-2));
        Assertions.assertTrue(reference.isCloseTo(referenceFromMJDMethod, 1e-2));
    }

    @Test
    void testLargeLeapSecond() {
        // this corresponds to issue 707
        Assertions.assertEquals(new AbsoluteDate(1961, 1, 1, utc).
                                shiftedBy(new TimeOffset(22818, TimeOffset.MICROSECOND).negate()),
                                new AbsoluteDate("1960-12-31T23:59:61.4", utc));
    }

    @Test
    void testGetDayOfYear() {
        Assertions.assertEquals(0.501,
                                new AbsoluteDate(2004,  1,  1,  0,  0,  0.001, utc).getDayOfYear(utc),
                                1.0e-3);
        Assertions.assertEquals(1.000,
                                new AbsoluteDate(2004,  1,  1, 12,  0,  0.000, utc).getDayOfYear(utc),
                                1.0e-3);
        Assertions.assertEquals(366.0,
                                new AbsoluteDate(2004, 12, 31, 12,  0,  0.000, utc).getDayOfYear(utc),
                                1.0e-3);
        Assertions.assertEquals(366.499999988426,
                                new AbsoluteDate(2004, 12, 31, 23, 59, 59.999, utc).getDayOfYear(utc),
                                1.0e-12);
        Assertions.assertEquals(0.500000011574,
                                new AbsoluteDate(2004, 12, 31, 23, 59, 59.999, utc).shiftedBy(0.002).getDayOfYear(utc),
                                1.0e-12);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
        present = new AbsoluteDate(new DateComponents(2000, 1, 1),
                                    new TimeComponents(12, 0, 0), utc);
        presentToo = new AnyTimeStamped(present.shiftedBy(0));
        past = new AnyTimeStamped(present.shiftedBy(-1000));
        future = new AnyTimeStamped(present.shiftedBy(1000));
    }

    private TimeScale utc;
    private AbsoluteDate present;
    private AnyTimeStamped past;
    private AnyTimeStamped presentToo;
    private AnyTimeStamped future;

    static class AnyTimeStamped implements TimeStamped {
        AbsoluteDate date;
        public AnyTimeStamped(AbsoluteDate date) {
            this.date = date;
        }

        @Override
        public AbsoluteDate getDate() {
            return date;
        }
    }
}
