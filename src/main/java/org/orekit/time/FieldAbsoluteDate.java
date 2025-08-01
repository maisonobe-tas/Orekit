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

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import java.util.concurrent.TimeUnit;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.FieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2Field;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.utils.Constants;

/** This class represents a specific instant in time.

 * <p>Instances of this class are considered to be absolute in the sense
 * that each one represent the occurrence of some event and can be compared
 * to other instances or located in <em>any</em> {@link TimeScale time scale}. In
 * other words the different locations of an event with respect to two different
 * time scales (say {@link TAIScale TAI} and {@link UTCScale UTC} for example) are
 * simply different perspective related to a single object. Only one
 * <code>FieldAbsoluteDate&lt;T&gt;</code> instance is needed, both representations being available
 * from this single instance by specifying the time scales as parameter when calling
 * the ad-hoc methods.</p>
 *
 * <p>Since an instance is not bound to a specific time-scale, all methods related
 * to the location of the date within some time scale require to provide the time
 * scale as an argument. It is therefore possible to define a date in one time scale
 * and to use it in another one. An example of such use is to read a date from a file
 * in UTC and write it in another file in TAI. This can be done as follows:</p>
 * <pre>
 *   DateTimeComponents utcComponents = readNextDate();
 *   FieldAbsoluteDate&lt;T&gt; date = new FieldAbsoluteDate&lt;&gt;(utcComponents, TimeScalesFactory.getUTC());
 *   writeNextDate(date.getComponents(TimeScalesFactory.getTAI()));
 * </pre>
 *
 * <p>Two complementary views are available:</p>
 * <ul>
 *   <li><p>location view (mainly for input/output or conversions)</p>
 *   <p>locations represent the coordinate of one event with respect to a
 *   {@link TimeScale time scale}. The related methods are {@link
 *   #FieldAbsoluteDate(Field, DateComponents, TimeComponents, TimeScale)}, {@link
 *   #FieldAbsoluteDate(Field, int, int, int, int, int, double, TimeScale)}, {@link
 *   #FieldAbsoluteDate(Field, int, int, int, TimeScale)}, {@link #FieldAbsoluteDate(Field,
 *   Date, TimeScale)}, {@link #createGPSDate(int, CalculusFieldElement)}, {@link
 *   #parseCCSDSCalendarSegmentedTimeCode(byte, byte[])}, {@link #toDate(TimeScale)},
 *   {@link #toString(TimeScale) toString(timeScale)}, {@link #toString()},
 *   and {@link #timeScalesOffset}.</p>
 *   </li>
 *   <li><p>offset view (mainly for physical computation)</p>
 *   <p>offsets represent either the flow of time between two events
 *   (two instances of the class) or durations. They are counted in seconds,
 *   are continuous and could be measured using only a virtually perfect stopwatch.
 *   The related methods are {@link #FieldAbsoluteDate(FieldAbsoluteDate, double)},
 *   {@link #parseCCSDSUnsegmentedTimeCode(Field, byte, byte, byte[], FieldAbsoluteDate)},
 *   {@link #parseCCSDSDaySegmentedTimeCode(Field, byte, byte[], DateComponents)},
 *   {@link #durationFrom(FieldAbsoluteDate)}, {@link #compareTo(FieldAbsoluteDate)}, {@link #equals(Object)}
 *   and {@link #hashCode()}.</p>
 *   </li>
 * </ul>
 * <p>
 * A few reference epochs which are commonly used in space systems have been defined. These
 * epochs can be used as the basis for offset computation. The supported epochs are:
 * {@link #getJulianEpoch(Field)}, {@link #getModifiedJulianEpoch(Field)}, {@link #getFiftiesEpoch(Field)},
 * {@link #getCCSDSEpoch(Field)}, {@link #getGalileoEpoch(Field)}, {@link #getGPSEpoch(Field)},
 * {@link #getJ2000Epoch(Field)}, {@link #getJavaEpoch(Field)}. There are also two factory methods
 * {@link #createJulianEpoch(CalculusFieldElement)} and {@link #createBesselianEpoch(CalculusFieldElement)}
 * that can be used to compute other reference epochs like J1900.0 or B1950.0.
 * In addition to these reference epochs, two other constants are defined for convenience:
 * {@link #getPastInfinity(Field)} and {@link #getFutureInfinity(Field)}, which can be used either
 * as dummy dates when a date is not yet initialized, or for initialization of loops searching for
 * a min or max date.
 * </p>
 * <p>
 * Instances of the <code>FieldAbsoluteDate&lt;T&gt;</code> class are guaranteed to be immutable.
 * </p>
 * @author Luc Maisonobe
 * @see TimeScale
 * @see TimeStamped
 * @see ChronologicalComparator
 * @param <T> type of the field elements
 */
public class FieldAbsoluteDate<T extends CalculusFieldElement<T>>
        implements FieldTimeStamped<T>, FieldTimeShiftable<FieldAbsoluteDate<T>, T>, Comparable<FieldAbsoluteDate<T>> {

    /** Underlying regular date.
     * @since 13.0
     */
    private final AbsoluteDate date;

    /** Field-specific offset ({@link CalculusFieldElement#getReal() is always 0)}.
     * @since 13.0
     */
    private final T fieldOffset;

    /** Build an instance from an AbsoluteDate.
     * @param field used by default
     * @param date AbsoluteDate to instantiate as a FieldAbsoluteDate
     */
    public FieldAbsoluteDate(final Field<T> field, final AbsoluteDate date) {
        this.date = date;
        this.fieldOffset = field.getZero();
    }

    /** Create an instance with a default value ({@link #getJ2000Epoch(Field)}).
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param field field used by default
     * @see #FieldAbsoluteDate(Field, AbsoluteDate)
     */
    @DefaultDataContext
    public FieldAbsoluteDate(final Field<T> field) {
        this.date        = AbsoluteDate.J2000_EPOCH;
        this.fieldOffset = field.getZero();
    }

    /** Build an instance from an elapsed duration since another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale. As an example,
     * the duration between the two instants leading to the readings
     * 2005-12-31T23:59:59 and 2006-01-01T00:00:00 in the {@link UTCScale UTC}
     * time scale is <em>not</em> 1 second, but a stop watch would have measured
     * an elapsed duration of 2 seconds between these two instances because a leap
     * second was introduced at the end of 2005 in this time scale.</p>
     * <p>This constructor is the reverse of the {@link #durationFrom(FieldAbsoluteDate)}
     * method.</p>
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     * @see #durationFrom(FieldAbsoluteDate)
     */
    public FieldAbsoluteDate(final FieldAbsoluteDate<T> since, final T elapsedDuration) {
        this.date        = since.date.shiftedBy(elapsedDuration.getReal());
        this.fieldOffset = since.fieldOffset.add(elapsedDuration.getAddendum());
    }

    /** Build an instance from a location (parsed from a string) in a {@link TimeScale time scale}.
     * <p>
     * The supported formats for location are mainly the ones defined in ISO-8601 standard,
     * the exact subset is explained in {@link DateTimeComponents#parseDateTime(String)},
     * {@link DateComponents#parseDate(String)} and {@link TimeComponents#parseTime(String)}.
     * </p>
     * <p>
     * As CCSDS ASCII calendar segmented time code is a trimmed down version of ISO-8601,
     * it is also supported by this constructor.
     * </p>
     * @param field field utilized by default
     * @param location location in the time scale, must be in a supported format
     * @param timeScale time scale
     * @exception IllegalArgumentException if location string is not in a supported format
     */
    public FieldAbsoluteDate(final Field<T> field, final String location, final TimeScale timeScale) {
        this(field, DateTimeComponents.parseDateTime(location), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public FieldAbsoluteDate(final Field<T> field, final DateTimeComponents location, final TimeScale timeScale) {
        this(field, location.getDate(), location.getTime(), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param date date location in the time scale
     * @param time time location in the time scale
     * @param timeScale time scale
     */
    public FieldAbsoluteDate(final Field<T> field, final DateComponents date, final TimeComponents time,
                             final TimeScale timeScale) {
        this.date        = new AbsoluteDate(date, time, timeScale);
        this.fieldOffset = field.getZero();
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final int month, final int day,
                             final int hour, final int minute, final double second,
                             final TimeScale timeScale) throws IllegalArgumentException {
        this(field, year, month, day, hour, minute, new TimeOffset(second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     * @since 13.0
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final int month, final int day,
                             final int hour, final int minute, final TimeOffset second,
                             final TimeScale timeScale) throws IllegalArgumentException {
        this(field, new DateComponents(year, month, day), new TimeComponents(hour, minute, second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final Month month, final int day,
                             final int hour, final int minute, final double second,
                             final TimeScale timeScale) throws IllegalArgumentException {
        this(field, year, month, day, hour, minute, new TimeOffset(second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param hour hour number from 0 to 23
     * @param minute minute number from 0 to 59
     * @param second second number from 0.0 to 60.0 (excluded)
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     * @since 13.0
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final Month month, final int day,
                             final int hour, final int minute, final TimeOffset second,
                             final TimeScale timeScale) throws IllegalArgumentException {
        this(field, new DateComponents(year, month, day), new TimeComponents(hour, minute, second), timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param field field utilized by default
     * @param date date location in the time scale
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final DateComponents date, final TimeScale timeScale)
                    throws IllegalArgumentException {
        this(field, date, TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month number from 1 to 12
     * @param day day number from 1 to 31
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final int month, final int day,
                             final TimeScale timeScale) throws IllegalArgumentException {
        this(field, new DateComponents(year, month, day), TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * <p>The hour is set to 00:00:00.000.</p>
     * @param field field utilized by default
     * @param year year number (may be 0 or negative for BC years)
     * @param month month enumerate
     * @param day day number from 1 to 31
     * @param timeScale time scale
     * @exception IllegalArgumentException if inconsistent arguments
     * are given (parameters out of range)
     */
    public FieldAbsoluteDate(final Field<T> field, final int year, final Month month, final int day,
                             final TimeScale timeScale) throws IllegalArgumentException {
        this(field, new DateComponents(year, month, day), TimeComponents.H00, timeScale);
    }

    /** Build an instance from a location in a {@link TimeScale time scale}.
     * @param field field utilized as default
     * @param location location in the time scale
     * @param timeScale time scale
     */
    public FieldAbsoluteDate(final Field<T> field, final Date location, final TimeScale timeScale) {
        this(field,
             new DateComponents(DateComponents.JAVA_EPOCH, (int) (location.getTime() / 86400000L)),
             new TimeComponents(new TimeOffset(location.getTime() % 86400000L, TimeOffset.MILLISECOND)),
             timeScale);
    }

    /** Build an instance from an {@link Instant instant} in a {@link TimeScale time scale}.
     * @param field field utilized as default
     * @param instant instant in the time scale
     * @param timeScale time scale
     * @since 12.0
     */
    public FieldAbsoluteDate(final Field<T> field, final Instant instant, final TimeScale timeScale) {
        this(field,
             new DateComponents(DateComponents.JAVA_EPOCH, (int) (instant.getEpochSecond() / 86400L)),
             new TimeComponents(new TimeOffset(instant.getEpochSecond() % 86400L, TimeOffset.SECOND,
                                               instant.getNano(), TimeOffset.NANOSECOND)),
             timeScale);
    }

    /** Build an instance from an {@link Instant instant} in utc time scale.
     * @param field field utilized as default
     * @param instant instant in the utc timescale
     * @since 12.1
     */
    @DefaultDataContext
    public FieldAbsoluteDate(final Field<T> field, final Instant instant) {
        this(field, instant, TimeScalesFactory.getUTC());
    }

    /** Build an instance from an {@link Instant instant} in the {@link UTCScale time scale}.
     * @param field field utilized as default
     * @param instant instant in the time scale
     * @param utcScale utc time scale
     * @since 12.1
     */
    public FieldAbsoluteDate(final Field<T> field, final Instant instant, final UTCScale utcScale) {
        this(field,
             new DateComponents(DateComponents.JAVA_EPOCH, (int) (instant.getEpochSecond() / 86400L)),
             new TimeComponents(new TimeOffset(instant.getEpochSecond() % 86400L, TimeOffset.SECOND,
                                               instant.getNano(), TimeOffset.NANOSECOND)),
            utcScale);
    }

    /** Build an instance from an elapsed duration since another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale.
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     */
    public FieldAbsoluteDate(final FieldAbsoluteDate<T> since, final double elapsedDuration) {
        this(since, new TimeOffset(elapsedDuration));
    }

    /** Build an instance from an elapsed duration since another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale.
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     * @since 13.0
     */
    public FieldAbsoluteDate(final FieldAbsoluteDate<T> since, final TimeOffset elapsedDuration) {
        this.date        = since.date.shiftedBy(elapsedDuration);
        this.fieldOffset = since.fieldOffset;
    }

    /** Build an instance from an elapsed duration since another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale.
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     * @param timeUnit {@link TimeUnit} of the elapsed duration
     * @since 12.1
     */
    public FieldAbsoluteDate(final FieldAbsoluteDate<T> since, final long elapsedDuration, final TimeUnit timeUnit) {
        this.date        = since.date.shiftedBy(elapsedDuration, timeUnit);
        this.fieldOffset = since.fieldOffset;
    }


    /** Build an instance from an elapsed duration since another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale.
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     */
    public FieldAbsoluteDate(final AbsoluteDate since, final T elapsedDuration) {
        this.date        = since.shiftedBy(elapsedDuration.getReal());
        this.fieldOffset = elapsedDuration.getAddendum();
    }

    /** Build an instance from an elapsed duration since another instant.
     * <p>It is important to note that the elapsed duration is <em>not</em>
     * the difference between two readings on a time scale.
     * @param since start instant of the measured duration
     * @param elapsedDuration physically elapsed duration from the <code>since</code>
     * instant, as measured in a regular time scale
     * @param timeUnit {@link TimeUnit} of the elapsed duration
     * @param field field utilized by default
     * @since 12.1
     */
    public FieldAbsoluteDate(final AbsoluteDate since, final long elapsedDuration, final TimeUnit timeUnit, final Field<T> field) {
        this.date        = since.shiftedBy(elapsedDuration, timeUnit);
        this.fieldOffset = field.getZero();
    }

    /** Build an instance from an apparent clock offset with respect to another
     * instant <em>in the perspective of a specific {@link TimeScale time scale}</em>.
     * <p>It is important to note that the apparent clock offset <em>is</em> the
     * difference between two readings on a time scale and <em>not</em> an elapsed
     * duration. As an example, the apparent clock offset between the two instants
     * leading to the readings 2005-12-31T23:59:59 and 2006-01-01T00:00:00 in the
     * {@link UTCScale UTC} time scale is 1 second, but the elapsed duration is 2
     * seconds because a leap second has been introduced at the end of 2005 in this
     * time scale.</p>
     * <p>This constructor is the reverse of the {@link #offsetFrom(FieldAbsoluteDate,
     * TimeScale)} method.</p>
     * @param reference reference instant
     * @param apparentOffset apparent clock offset from the reference instant
     * (difference between two readings in the specified time scale)
     * @param timeScale time scale with respect to which the offset is defined
     * @see #offsetFrom(FieldAbsoluteDate, TimeScale)
     */
    public FieldAbsoluteDate(final FieldAbsoluteDate<T> reference, final double apparentOffset, final TimeScale timeScale) {
        this(reference.fieldOffset.getField(),
             new DateTimeComponents(reference.getComponents(timeScale), apparentOffset),
             timeScale);
    }

    /** Creates Field date with offset as univariate derivative of first order, with a unit linear coefficient in time.
     * @return univariate derivative 1 date
     * @since 13.1
     */
    public FieldAbsoluteDate<FieldUnivariateDerivative1<T>> toFUD1Field() {
        final FieldUnivariateDerivative1Field<T> fud1Field = FieldUnivariateDerivative1Field.getUnivariateDerivative1Field(fieldOffset.getField());
        final FieldUnivariateDerivative1<T> fud1Shift = new FieldUnivariateDerivative1<>(fieldOffset,
                fieldOffset.getField().getOne());
        return new FieldAbsoluteDate<>(fud1Field, date).shiftedBy(fud1Shift);
    }

    /** Creates Field date with offset as univariate derivative of second order, with a unit linear coefficient in time.
     * @return univariate derivative 2 date
     * @since 12.2
     */
    public FieldAbsoluteDate<FieldUnivariateDerivative2<T>> toFUD2Field() {
        final FieldUnivariateDerivative2Field<T> fud2Field = FieldUnivariateDerivative2Field.getUnivariateDerivative2Field(fieldOffset.getField());
        final FieldUnivariateDerivative2<T> fud2Shift = new FieldUnivariateDerivative2<>(fieldOffset,
                                                                                         fieldOffset.getField().getOne(),
                                                                                         fieldOffset.getField().getZero());
        return new FieldAbsoluteDate<>(fud2Field, date).shiftedBy(fud2Shift);
    }

    /** Build an instance from a CCSDS Unsegmented Time Code (CUC).
     * <p>
     * CCSDS Unsegmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * <p>
     * If the date to be parsed is formatted using version 3 of the standard
     * (CCSDS 301.0-B-3 published in 2002) or if the extension of the preamble
     * field introduced in version 4 of the standard is not used, then the
     * {@code preambleField2} parameter can be set to 0.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context} if
     * the CCSDS epoch is used.
     *
     * @param field field for the components
     * @param preambleField1 first byte of the field specifying the format, often
     * not transmitted in data interfaces, as it is constant for a given data interface
     * @param preambleField2 second byte of the field specifying the format
     * (added in revision 4 of the CCSDS standard in 2010), often not transmitted in data
     * interfaces, as it is constant for a given data interface (value ignored if presence
     * not signaled in {@code preambleField1})
     * @param timeField byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field
     * specifies the {@link #getCCSDSEpoch(Field) CCSDS reference epoch} is used (and hence
     * may be null in this case)
     * @return an instance corresponding to the specified date
     * @param <T> the type of the field elements
     * @see #parseCCSDSUnsegmentedTimeCode(byte, byte, byte[], FieldAbsoluteDate,
     * FieldAbsoluteDate)
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> parseCCSDSUnsegmentedTimeCode(final Field<T> field,
                                                                                                         final byte preambleField1,
                                                                                                         final byte preambleField2,
                                                                                                         final byte[] timeField,
                                                                                                         final FieldAbsoluteDate<T> agencyDefinedEpoch) {
        return parseCCSDSUnsegmentedTimeCode(preambleField1, preambleField2,
                                             timeField, agencyDefinedEpoch,
                                             new FieldAbsoluteDate<>(field,
                                                                     DataContext.getDefault().getTimeScales().getCcsdsEpoch()));
    }

    /**
     * Build an instance from a CCSDS Unsegmented Time Code (CUC).
     * <p>
     * CCSDS Unsegmented Time Code is defined in the blue book: CCSDS Time Code Format
     * (CCSDS 301.0-B-4) published in November 2010
     * </p>
     * <p>
     * If the date to be parsed is formatted using version 3 of the standard (CCSDS
     * 301.0-B-3 published in 2002) or if the extension of the preamble field introduced
     * in version 4 of the standard is not used, then the {@code preambleField2} parameter
     * can be set to 0.
     * </p>
     *
     * @param <T>                the type of the field elements
     * @param preambleField1     first byte of the field specifying the format, often not
     *                           transmitted in data interfaces, as it is constant for a
     *                           given data interface
     * @param preambleField2     second byte of the field specifying the format (added in
     *                           revision 4 of the CCSDS standard in 2010), often not
     *                           transmitted in data interfaces, as it is constant for a
     *                           given data interface (value ignored if presence not
     *                           signaled in {@code preambleField1})
     * @param timeField          byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field specifies
     *                           the {@link DateComponents#CCSDS_EPOCH CCSDS reference epoch} is used
     *                           (and hence may be null in this case, but then {@code ccsdsEpoch} must be non-null)
     * @param ccsdsEpoch         reference epoch, ignored if the preamble field specifies
     *                           the agency epoch is used (and hence may be null in this case,
     *                           but then {@code agencyDefinedEpoch} must be non-null).
     * @return an instance corresponding to the specified date
     * @since 10.1
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> parseCCSDSUnsegmentedTimeCode(final byte preambleField1,
                                                                                                         final byte preambleField2,
                                                                                                         final byte[] timeField,
                                                                                                         final FieldAbsoluteDate<T> agencyDefinedEpoch,
                                                                                                         final FieldAbsoluteDate<T> ccsdsEpoch) {
        final CcsdsUnsegmentedTimeCode<FieldAbsoluteDate<T>> timeCode =
            new CcsdsUnsegmentedTimeCode<>(preambleField1, preambleField2, timeField, agencyDefinedEpoch, ccsdsEpoch);
        return timeCode.getEpoch().shiftedBy(timeCode.getTime());
    }

    /** Build an instance from a CCSDS Day Segmented Time Code (CDS).
     * <p>
     * CCSDS Day Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param field field for the components
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field
     * specifies the {@link #getCCSDSEpoch(Field) CCSDS reference epoch} is used (and hence
     * may be null in this case)
     * @return an instance corresponding to the specified date
     * @param <T> the type of the field elements
     * @see #parseCCSDSDaySegmentedTimeCode(Field, byte, byte[], DateComponents,
     * TimeScale)
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> parseCCSDSDaySegmentedTimeCode(final Field<T> field,
                                                                                                          final byte preambleField, final byte[] timeField,
                                                                                                          final DateComponents agencyDefinedEpoch) {
        return parseCCSDSDaySegmentedTimeCode(field, preambleField, timeField,
                                              agencyDefinedEpoch, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Build an instance from a CCSDS Day Segmented Time Code (CDS).
     * <p>
     * CCSDS Day Segmented Time Code is defined in the blue book: CCSDS Time Code Format
     * (CCSDS 301.0-B-4) published in November 2010
     * </p>
     *
     * @param <T>                the type of the field elements
     * @param field              field for the components
     * @param preambleField      field specifying the format, often not transmitted in
     *                           data interfaces, as it is constant for a given data
     *                           interface
     * @param timeField          byte array containing the time code
     * @param agencyDefinedEpoch reference epoch, ignored if the preamble field specifies
     *                           the {@link #getCCSDSEpoch(Field) CCSDS reference epoch}
     *                           is used (and hence may be null in this case)
     * @param utc                time scale used to compute date and time components.
     * @return an instance corresponding to the specified date
     * @since 10.1
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> parseCCSDSDaySegmentedTimeCode(final Field<T> field,
                                                                                                          final byte preambleField,
                                                                                                          final byte[] timeField,
                                                                                                          final DateComponents agencyDefinedEpoch,
                                                                                                          final TimeScale utc) {
        final CcsdsSegmentedTimeCode timeCode = new CcsdsSegmentedTimeCode(preambleField, timeField, agencyDefinedEpoch);
        return new FieldAbsoluteDate<>(field, timeCode.getDate(), timeCode.getTime(), utc);
    }

    /** Build an instance from a CCSDS Calendar Segmented Time Code (CCS).
     * <p>
     * CCSDS Calendar Segmented Time Code is defined in the blue book:
     * CCSDS Time Code Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param preambleField field specifying the format, often not transmitted in
     * data interfaces, as it is constant for a given data interface
     * @param timeField byte array containing the time code
     * @return an instance corresponding to the specified date
     * @see #parseCCSDSCalendarSegmentedTimeCode(byte, byte[], TimeScale)
     */
    @DefaultDataContext
    public FieldAbsoluteDate<T> parseCCSDSCalendarSegmentedTimeCode(final byte preambleField, final byte[] timeField) {
        return parseCCSDSCalendarSegmentedTimeCode(preambleField, timeField,
                                                   DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Build an instance from a CCSDS Calendar Segmented Time Code (CCS).
     * <p>
     * CCSDS Calendar Segmented Time Code is defined in the blue book: CCSDS Time Code
     * Format (CCSDS 301.0-B-4) published in November 2010
     * </p>
     *
     * @param preambleField field specifying the format, often not transmitted in data
     *                      interfaces, as it is constant for a given data interface
     * @param timeField     byte array containing the time code
     * @param utc           time scale used to compute date and time components.
     * @return an instance corresponding to the specified date
     * @since 10.1
     */
    public FieldAbsoluteDate<T> parseCCSDSCalendarSegmentedTimeCode(final byte preambleField,
                                                                    final byte[] timeField,
                                                                    final TimeScale utc) {
        final CcsdsSegmentedTimeCode timeCode = new CcsdsSegmentedTimeCode(preambleField, timeField);
        return new FieldAbsoluteDate<>(fieldOffset.getField(), timeCode.getDate(), timeCode.getTime(), utc);
    }

    /** Build an instance corresponding to a Julian Day date.
     * @param jd Julian day
     * @param secondsSinceNoon seconds in the Julian day
     * (BEWARE, Julian days start at noon, so 0.0 is noon)
     * @param timeScale time scale in which the seconds in day are defined
     * @return a new instant
     * @param <T> the type of the field elements
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createJDDate(final int jd, final T secondsSinceNoon,
                                                                                        final TimeScale timeScale) {
        return new FieldAbsoluteDate<>(secondsSinceNoon.getField(), new DateComponents(DateComponents.JULIAN_EPOCH, jd),
                        TimeComponents.H12, timeScale).shiftedBy(secondsSinceNoon);
    }

    /** Build an instance corresponding to a Julian Day date.
     * <p>
     * This function should be preferred to {@link #createJDDate(int, CalculusFieldElement, TimeScale)} when the target time scale
     * has a non-constant offset with respect to TAI.
     * <p>
     * The idea is to introduce a pivot time scale that is close to the target time scale but has a constant bias with TAI.
     * <p>
     * For example, to get a date from an MJD in TDB time scale, it's advised to use the TT time scale
     * as a pivot scale. TT is very close to TDB and has constant offset to TAI.
     * </p>
     * @param jd Julian day
     * @param secondsSinceNoon seconds in the Julian day
     * (BEWARE, Julian days start at noon, so 0.0 is noon)
     * @param timeScale time scale in which the seconds in day are defined
     * @param pivotTimeScale pivot timescale used as intermediate timescale
     * @return a new instant
     * @param <T> the type of the field elements
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createJDDate(final int jd, final T secondsSinceNoon,
                                                                                        final TimeScale timeScale,
                                                                                        final TimeScale pivotTimeScale) {
        // Get the date in pivot timescale
        final FieldAbsoluteDate<T> dateInPivotTimeScale = createJDDate(jd, secondsSinceNoon, pivotTimeScale);

        // Compare offsets to TAI of the two time scales
        final T offsetFromTAI = timeScale.offsetFromTAI(dateInPivotTimeScale).
                subtract(pivotTimeScale.offsetFromTAI(dateInPivotTimeScale));

        // Return date in desired timescale
        return dateInPivotTimeScale.shiftedBy(offsetFromTAI.multiply(-1.));
    }

    /** Build an instance corresponding to a Modified Julian Day date.
     * @param mjd modified Julian day
     * @param secondsInDay seconds in the day
     * @param timeScale time scale in which the seconds in day are defined
     * @return a new instant
     * @param <T> the type of the field elements
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createMJDDate(final int mjd, final T secondsInDay,
                                                                                         final TimeScale timeScale) {
        return new FieldAbsoluteDate<>(secondsInDay.getField(),
                                       new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                       TimeComponents.H00,
                                       timeScale).shiftedBy(secondsInDay);
    }

    /** Create an instance as the median data between two existing instances.
     * @param date1 first instance
     * @param date2 second instance
     * @return median date between first and second instance
     * @param <T> the type of the field elements
     * @since 13.0
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createMedian(final FieldAbsoluteDate<T> date1,
                                                                                        final FieldAbsoluteDate<T> date2) {
        return new FieldAbsoluteDate<>(AbsoluteDate.createMedian(date1.date, date2.date),
                                       date2.fieldOffset.add(date1.fieldOffset).multiply(0.5));
    }

    /** Build an instance corresponding to a GPS date.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * <p>GPS dates are provided as a week number starting at
     * {@link #getGPSEpoch(Field) GPS epoch} and as a number of milliseconds
     * since week start.</p>
     * @param weekNumber week number since {@link #getGPSEpoch(Field) GPS epoch}
     * @param milliInWeek number of milliseconds since week start
     * @return a new instant
     * @param <T> the type of the field elements
     * @see #createGPSDate(int, CalculusFieldElement, TimeScale)
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createGPSDate(final int weekNumber, final T milliInWeek) {
        return createGPSDate(weekNumber, milliInWeek,
                             DataContext.getDefault().getTimeScales().getGPS());
    }

    /**
     * Build an instance corresponding to a GPS date.
     * <p>GPS dates are provided as a week number starting at
     * {@link #getGPSEpoch(Field) GPS epoch} and as a number of milliseconds since week
     * start.</p>
     *
     * @param <T>         the type of the field elements
     * @param weekNumber  week number since {@link #getGPSEpoch(Field) GPS epoch}
     * @param milliInWeek number of milliseconds since week start
     * @param gps         GPS time scale.
     * @return a new instant
     * @since 10.1
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createGPSDate(
                                                                                         final int weekNumber,
                                                                                         final T milliInWeek,
                                                                                         final TimeScale gps) {

        final int day = (int) FastMath.floor(milliInWeek.getReal() / (1000.0 * Constants.JULIAN_DAY));
        final T secondsInDay = milliInWeek.divide(1000.0).subtract(day * Constants.JULIAN_DAY);
        return new FieldAbsoluteDate<>(milliInWeek.getField(),
                                       new DateComponents(DateComponents.GPS_EPOCH, weekNumber * 7 + day),
                                       TimeComponents.H00, gps).
               shiftedBy(secondsInDay);
    }

    /** Build an instance corresponding to a Julian Epoch (JE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&amp;defaultprint=YES&amp;filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>, Astronomy and Astrophysics,
     * vol. 73, no. 3, Mar. 1979, p. 282-284, Julian Epoch is related to Julian Ephemeris Date as:
     * <pre>JE = 2000.0 + (JED - 2451545.0) / 365.25</pre>
     * <p>This method reverts the formula above and computes an {@code FieldAbsoluteDate<T>} from the Julian Epoch.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param <T> the type of the field elements
     * @param julianEpoch Julian epoch, like 2000.0 for defining the classical reference J2000.0
     * @return a new instant
     * @see #getJ2000Epoch(Field)
     * @see #createBesselianEpoch(CalculusFieldElement)
     * @see #createJulianEpoch(CalculusFieldElement, TimeScales)
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createJulianEpoch(final T julianEpoch) {
        return createJulianEpoch(julianEpoch, DataContext.getDefault().getTimeScales());
    }

    /**
     * Build an instance corresponding to a Julian Epoch (JE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&amp;defaultprint=YES&amp;filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>,
     * Astronomy and Astrophysics, vol. 73, no. 3, Mar. 1979, p. 282-284, Julian Epoch is
     * related to Julian Ephemeris Date as:
     * <pre>JE = 2000.0 + (JED - 2451545.0) / 365.25</pre>
     * <p>This method reverts the formula above and computes an {@code
     * FieldAbsoluteDate<T>} from the Julian Epoch.
     *
     * @param <T>         the type of the field elements
     * @param julianEpoch Julian epoch, like 2000.0 for defining the classical reference
     *                    J2000.0
     * @param timeScales  used in the computation.
     * @return a new instant
     * @see #getJ2000Epoch(Field)
     * @see #createBesselianEpoch(CalculusFieldElement)
     * @see TimeScales#createJulianEpoch(double)
     * @since 10.1
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createJulianEpoch(
                                                                                             final T julianEpoch,
                                                                                             final TimeScales timeScales) {
        final Field<T> field = julianEpoch.getField();
        return new FieldAbsoluteDate<>(new FieldAbsoluteDate<>(field, timeScales.getJ2000Epoch()),
                                       julianEpoch.subtract(2000.0).multiply(Constants.JULIAN_YEAR));
    }

    /** Build an instance corresponding to a Besselian Epoch (BE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&amp;defaultprint=YES&amp;filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>, Astronomy and Astrophysics,
     * vol. 73, no. 3, Mar. 1979, p. 282-284, Besselian Epoch is related to Julian Ephemeris Date as:</p>
     * <pre>
     * BE = 1900.0 + (JED - 2415020.31352) / 365.242198781
     * </pre>
     * <p>
     * This method reverts the formula above and computes an {@code FieldAbsoluteDate<T>} from the Besselian Epoch.
     * </p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param <T> the type of the field elements
     * @param besselianEpoch Besselian epoch, like 1950 for defining the classical reference B1950.0
     * @return a new instant
     * @see #createJulianEpoch(CalculusFieldElement)
     * @see #createBesselianEpoch(CalculusFieldElement, TimeScales)
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createBesselianEpoch(final T besselianEpoch) {
        return createBesselianEpoch(besselianEpoch, DataContext.getDefault().getTimeScales());
    }

    /**
     * Build an instance corresponding to a Besselian Epoch (BE).
     * <p>According to Lieske paper: <a
     * href="http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&amp;defaultprint=YES&amp;filetype=.pdf.">
     * Precession Matrix Based on IAU (1976) System of Astronomical Constants</a>,
     * Astronomy and Astrophysics, vol. 73, no. 3, Mar. 1979, p. 282-284, Besselian Epoch
     * is related to Julian Ephemeris Date as:</p>
     * <pre>
     * BE = 1900.0 + (JED - 2415020.31352) / 365.242198781
     * </pre>
     * <p>
     * This method reverts the formula above and computes an {@code FieldAbsoluteDate<T>}
     * from the Besselian Epoch.
     * </p>
     *
     * @param <T>            the type of the field elements
     * @param besselianEpoch Besselian epoch, like 1950 for defining the classical
     *                       reference B1950.0
     * @param timeScales     used in the computation.
     * @return a new instant
     * @see #createJulianEpoch(CalculusFieldElement)
     * @see TimeScales#createBesselianEpoch(double)
     * @since 10.1
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> createBesselianEpoch(
                                                                                                final T besselianEpoch,
                                                                                                final TimeScales timeScales) {
        final Field<T> field = besselianEpoch.getField();
        return new FieldAbsoluteDate<>(new FieldAbsoluteDate<>(field, timeScales.getJ2000Epoch()),
                                       besselianEpoch.subtract(1900).multiply(Constants.BESSELIAN_YEAR).
                                           add(Constants.JULIAN_DAY * (-36525) + Constants.JULIAN_DAY * 0.31352));
    }

    /** Reference epoch for julian dates: -4712-01-01T12:00:00 Terrestrial Time.
     * <p>Both <code>java.util.Date</code> and {@link DateComponents} classes
     * follow the astronomical conventions and consider a year 0 between
     * years -1 and +1, hence this reference date lies in year -4712 and not
     * in year -4713 as can be seen in other documents or programs that obey
     * a different convention (for example the <code>convcal</code> utility).</p>
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return the reference epoch for julian dates as a FieldAbsoluteDate
     * @see AbsoluteDate#JULIAN_EPOCH
     * @see TimeScales#getJulianEpoch()
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getJulianEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, DataContext.getDefault().getTimeScales().getJulianEpoch());
    }

    /** Reference epoch for modified julian dates: 1858-11-17T00:00:00 Terrestrial Time.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return the reference epoch for modified julian dates as a FieldAbsoluteDate
     * @see AbsoluteDate#MODIFIED_JULIAN_EPOCH
     * @see TimeScales#getModifiedJulianEpoch()
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getModifiedJulianEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, DataContext.getDefault().getTimeScales().getModifiedJulianEpoch());
    }

    /** Reference epoch for 1950 dates: 1950-01-01T00:00:00 Terrestrial Time.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return the reference epoch for 1950 dates as a FieldAbsoluteDate
     * @see AbsoluteDate#FIFTIES_EPOCH
     * @see TimeScales#getFiftiesEpoch()
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getFiftiesEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, DataContext.getDefault().getTimeScales().getFiftiesEpoch());
    }

    /** Reference epoch for CCSDS Time Code Format (CCSDS 301.0-B-4).
     * <p>
     * This method uses the {@link DataContext#getDefault() default data context}.
     * </p>
     * 1958-01-01T00:00:00 International Atomic Time (<em>not</em> UTC).
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return the reference epoch for CCSDS Time Code Format as a FieldAbsoluteDate
     * @see AbsoluteDate#CCSDS_EPOCH
     * @see TimeScales#getCcsdsEpoch()
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getCCSDSEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, DataContext.getDefault().getTimeScales().getCcsdsEpoch());
    }

    /** Reference epoch for Galileo System Time: 1999-08-22T00:00:00 UTC.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return the reference epoch for Galileo System Time as a FieldAbsoluteDate
     * @see AbsoluteDate#GALILEO_EPOCH
     * @see TimeScales#getGalileoEpoch()
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getGalileoEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, DataContext.getDefault().getTimeScales().getGalileoEpoch());
    }

    /** Reference epoch for GPS weeks: 1980-01-06T00:00:00 GPS time.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return the reference epoch for GPS weeks as a FieldAbsoluteDate
     * @see AbsoluteDate#GPS_EPOCH
     * @see TimeScales#getGpsEpoch()
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getGPSEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, DataContext.getDefault().getTimeScales().getGpsEpoch());
    }

    /** J2000.0 Reference epoch: 2000-01-01T12:00:00 Terrestrial Time (<em>not</em> UTC).
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return the J2000.0 reference epoch as a FieldAbsoluteDate
     * @see #createJulianEpoch(CalculusFieldElement)
     * @see AbsoluteDate#J2000_EPOCH
     * @see TimeScales#getJ2000Epoch()
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getJ2000Epoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, DataContext.getDefault().getTimeScales().getJ2000Epoch());
    }

    /** Java Reference epoch: 1970-01-01T00:00:00 Universal Time Coordinate.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * <p>
     * Between 1968-02-01 and 1972-01-01, UTC-TAI = 4.213 170 0s + (MJD - 39 126) x 0.002 592s.
     * As on 1970-01-01 MJD = 40587, UTC-TAI = 8.000082s
     * </p>
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return the Java reference epoch as a FieldAbsoluteDate
     * @see AbsoluteDate#JAVA_EPOCH
     * @see TimeScales#getJavaEpoch()
     */
    @DefaultDataContext
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getJavaEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, DataContext.getDefault().getTimeScales().getJavaEpoch());
    }

    /** Dummy date at infinity in the past direction.
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return a dummy date at infinity in the past direction as a FieldAbsoluteDate
     * @see AbsoluteDate#PAST_INFINITY
     * @see TimeScales#getPastInfinity()
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getPastInfinity(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.PAST_INFINITY);
    }

    /** Dummy date at infinity in the future direction.
     * @param <T> the type of the field elements
     * @param field field for the components
     * @return a dummy date at infinity in the future direction as a FieldAbsoluteDate
     * @see AbsoluteDate#FUTURE_INFINITY
     * @see TimeScales#getFutureInfinity()
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getFutureInfinity(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.FUTURE_INFINITY);
    }

    /**
     * Get an arbitrary date. Useful when a non-null date is needed but its values does
     * not matter.
     *
     * @param <T>   the type of the field elements
     * @param field field for the components
     * @return an arbitrary date.
     */
    public static <T extends CalculusFieldElement<T>> FieldAbsoluteDate<T> getArbitraryEpoch(final Field<T> field) {
        return new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
    }


    /** Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call {@code new FieldAbsoluteDate&lt;&gt;(this, dt)}.
     * </p>
     * @param dt time shift in seconds
     * @return a new date, shifted with respect to instance (which is immutable)
     * @see org.orekit.utils.FieldPVCoordinates#shiftedBy(double)
     * @see org.orekit.attitudes.FieldAttitude#shiftedBy(double)
     * @see org.orekit.orbits.FieldOrbit#shiftedBy(double)
     * @see org.orekit.propagation.FieldSpacecraftState#shiftedBy(double)
     */
    @Override
    public FieldAbsoluteDate<T> shiftedBy(final T dt) {
        return new FieldAbsoluteDate<>(this, dt);
    }

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the number of seconds physically
     * elapsed between the two instants, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * <p>This method gives the same result (with less computation)
     * as calling {@link #offsetFrom(FieldAbsoluteDate, TimeScale)}
     * with a second argument set to one of the regular scales cited
     * above.</p>
     * <p>This method is the reverse of the {@link #FieldAbsoluteDate(FieldAbsoluteDate,
     * double)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     * if the instance is posterior to the argument)
     * @see #offsetFrom(FieldAbsoluteDate, TimeScale)
     * @see #FieldAbsoluteDate(FieldAbsoluteDate, double)
     */
    public T durationFrom(final FieldAbsoluteDate<T> instant) {
        return fieldOffset.subtract(instant.fieldOffset).
               add(date.durationFrom(instant.date));
    }

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the number of seconds physically
     * elapsed between the two instants, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * <p>This method gives the same result (with less computation)
     * as calling {@link #offsetFrom(FieldAbsoluteDate, TimeScale)}
     * with a second argument set to one of the regular scales cited
     * above.</p>
     * <p>This method is the reverse of the {@link #FieldAbsoluteDate(FieldAbsoluteDate,
     * double)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @param timeUnit {@link TimeUnit} precision for the offset
     * @return offset in seconds between the two instants (positive
     * if the instance is posterior to the argument)
     * @see #offsetFrom(FieldAbsoluteDate, TimeScale)
     * @see #FieldAbsoluteDate(FieldAbsoluteDate, double)
     */
    public T durationFrom(final FieldAbsoluteDate<T> instant, final TimeUnit timeUnit) {
        return fieldOffset.subtract(instant.fieldOffset).
               add(date.durationFrom(instant.date, timeUnit));
    }

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the number of seconds physically
     * elapsed between the two instants, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * <p>This method gives the same result (with less computation)
     * as calling {@link #offsetFrom(FieldAbsoluteDate, TimeScale)}
     * with a second argument set to one of the regular scales cited
     * above.</p>
     * <p>This method is the reverse of the {@link #FieldAbsoluteDate(FieldAbsoluteDate,
     * double)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @return offset in seconds between the two instants (positive
     * if the instance is posterior to the argument)
     * @see #offsetFrom(FieldAbsoluteDate, TimeScale)
     * @see #FieldAbsoluteDate(FieldAbsoluteDate, double)
     */
    public T durationFrom(final AbsoluteDate instant) {
        return fieldOffset.add(date.durationFrom(instant));
    }

    /** Compute the physically elapsed duration between two instants.
     * <p>The returned duration is the number of seconds physically
     * elapsed between the two instants, measured in a regular time
     * scale with respect to surface of the Earth (i.e either the {@link
     * TAIScale TAI scale}, the {@link TTScale TT scale} or the {@link
     * GPSScale GPS scale}). It is the only method that gives a
     * duration with a physical meaning.</p>
     * <p>This method gives the same result (with less computation)
     * as calling {@link #offsetFrom(FieldAbsoluteDate, TimeScale)}
     * with a second argument set to one of the regular scales cited
     * above.</p>
     * <p>This method is the reverse of the {@link #FieldAbsoluteDate(FieldAbsoluteDate,
     * double)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @param timeUnit {@link TimeUnit} precision for the offset
     * @return offset in the given timeunit between the two instants (positive
     * if the instance is posterior to the argument), rounded to the nearest integer {@link TimeUnit}
     * @see #FieldAbsoluteDate(FieldAbsoluteDate, long, TimeUnit)
     * @since 12.1
     */
    public T durationFrom(final AbsoluteDate instant, final TimeUnit timeUnit) {
        return fieldOffset.add(date.durationFrom(instant, timeUnit));
    }

    /** Compute the apparent clock offset between two instant <em>in the
     * perspective of a specific {@link TimeScale time scale}</em>.
     * <p>The offset is the number of seconds counted in the given
     * time scale between the locations of the two instants, with
     * all time scale irregularities removed (i.e. considering all
     * days are exactly 86400 seconds long). This method will give
     * a result that may not have a physical meaning if the time scale
     * is irregular. For example since a leap second was introduced at
     * the end of 2005, the apparent offset between 2005-12-31T23:59:59
     * and 2006-01-01T00:00:00 is 1 second, but the physical duration
     * of the corresponding time interval as returned by the {@link
     * #durationFrom(FieldAbsoluteDate)} method is 2 seconds.</p>
     * <p>This method is the reverse of the {@link #FieldAbsoluteDate(FieldAbsoluteDate,
     * double, TimeScale)} constructor.</p>
     * @param instant instant to subtract from the instance
     * @param timeScale time scale with respect to which the offset should
     * be computed
     * @return apparent clock offset in seconds between the two instants
     * (positive if the instance is posterior to the argument)
     * @see #durationFrom(FieldAbsoluteDate)
     * @see #FieldAbsoluteDate(FieldAbsoluteDate, double, TimeScale)
     */
    public T offsetFrom(final FieldAbsoluteDate<T> instant, final TimeScale timeScale) {
        return fieldOffset.subtract(instant.fieldOffset).
               add(date.offsetFrom(instant.date, timeScale));
    }

    /** Compute the offset between two time scales at the current instant.
     * <p>The offset is defined as <i>l₁-l₂</i>
     * where <i>l₁</i> is the location of the instant in
     * the <code>scale1</code> time scale and <i>l₂</i> is the
     * location of the instant in the <code>scale2</code> time scale.</p>
     * @param scale1 first time scale
     * @param scale2 second time scale
     * @return offset in seconds between the two time scales at the
     * current instant
     */
    public T timeScalesOffset(final TimeScale scale1, final TimeScale scale2) {
        return scale1.offsetFromTAI(this).subtract(scale2.offsetFromTAI(this));
    }

    /** Convert the instance to a Java {@link java.util.Date Date}.
     * <p>Conversion to the Date class induces a loss of precision because
     * the Date class does not provide sub-millisecond information. Java Dates
     * are considered to be locations in some times scales.</p>
     * @param timeScale time scale to use
     * @return a {@link java.util.Date Date} instance representing the location
     * of the instant in the time scale
     */
    public Date toDate(final TimeScale timeScale) {
        return date.toDate(timeScale);
    }

    /**
     * Convert the instance to a Java {@link java.time.Instant Instant}.
     * Nanosecond precision is preserved during this conversion
     *
     * @return a {@link java.time.Instant Instant} instance representing the location
     * of the instant in the utc time scale
     * @since 12.1
     */
    @DefaultDataContext
    public Instant toInstant() {
        return toInstant(TimeScalesFactory.getTimeScales());
    }

    /**
     * Convert the instance to a Java {@link java.time.Instant Instant}.
     * Nanosecond precision is preserved during this conversion
     *
     * @param timeScales the timescales to use
     * @return a {@link java.time.Instant Instant} instance representing the location
     * of the instant in the utc time scale
     * @since 12.1
     */
    public Instant toInstant(final TimeScales timeScales) {
        return date.toInstant(timeScales);
    }

    /** Split the instance into date/time components.
     * @param timeScale time scale to use
     * @return date/time components
     */
    public DateTimeComponents getComponents(final TimeScale timeScale) {
        return date.getComponents(timeScale);
    }

    /** Split the instance into date/time components for a local time.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     * negative Westward UTC)
     * @return date/time components
     * @see #getComponents(int, TimeScale)
     */
    @DefaultDataContext
    public DateTimeComponents getComponents(final int minutesFromUTC) {
        return date.getComponents(minutesFromUTC);
    }

    /**
     * Split the instance into date/time components for a local time.
     *
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     *                       negative Westward UTC)
     * @param utc            time scale used to compute date and time components.
     * @return date/time components
     * @since 10.1
     */
    public DateTimeComponents getComponents(final int minutesFromUTC, final TimeScale utc) {
        return date.getComponents(minutesFromUTC, utc);
    }

    /** {@inheritDoc} */
    @Override
    public FieldAbsoluteDate<T> getDate() {
        return this;
    }

    /** Get the field.
     * @return field instance.
     */
    public Field<T> getField() {
        return fieldOffset.getField();
    }

    /** Split the instance into date/time components for a time zone.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param timeZone time zone
     * @return date/time components
     * @see #getComponents(TimeZone, TimeScale)
     */
    @DefaultDataContext
    public DateTimeComponents getComponents(final TimeZone timeZone) {
        return date.getComponents(timeZone);
    }

    /** Split the instance into date/time components for a time zone.
     * @param timeZone time zone
     * @param utc            time scale used to compute date and time components.
     * @return date/time components
     * @since 10.1
     */
    public DateTimeComponents getComponents(final TimeZone timeZone, final TimeScale utc) {
        return date.getComponents(timeZone, utc);
    }

    /** Compare the instance with another date.
     * @param other other date to compare the instance to
     * @return a negative integer, zero, or a positive integer as this date
     * is before, simultaneous, or after the specified date.
     */
    public int compareTo(final FieldAbsoluteDate<T> other) {
        return date.compareTo(other.date);
    }


    /** Check if the instance represents the same time as another instance.
     * @param other other date
     * @return true if the instance and the other date refer to the same instant with same Field and addendum
     */
    public boolean equals(final Object other) {

        if (other == this) {
            // first fast check
            return true;
        }

        if (other instanceof FieldAbsoluteDate) {
            final FieldAbsoluteDate<?> otherF = (FieldAbsoluteDate<?>) other;
            return fieldOffset.getField().equals(otherF.fieldOffset.getField()) &&
                   date.equals(otherF.date) && fieldOffset.getAddendum().equals(otherF.fieldOffset.getAddendum());
        }

        return false;

    }

    /** Check if the instance represents the same time as another.
     * @param other the instant to compare this date to
     * @return true if the instance and the argument refer to the same instant
     * @see #isCloseTo(FieldTimeStamped, double)
     * @since 10.1
     */
    public boolean isEqualTo(final FieldTimeStamped<T> other) {
        return this.equals(other.getDate());
    }

    /** Check if the instance time is close to another.
     * @param other the instant to compare this date to
     * @param tolerance the separation, in seconds, under which the two instants will be considered close to each other
     * @return true if the duration between the instance and the argument is strictly below the tolerance
     * @see #isEqualTo(FieldTimeStamped)
     * @since 10.1
     */
    public boolean isCloseTo(final FieldTimeStamped<T> other, final double tolerance) {
        return date.isCloseTo(other.getDate().date, tolerance);
    }

    /** Check if the instance represents a time that is strictly before another.
     * @param other the instant to compare this date to
     * @return true if the instance is strictly before the argument when ordering chronologically
     * @see #isBeforeOrEqualTo(FieldTimeStamped)
     * @since 10.1
     */
    public boolean isBefore(final FieldTimeStamped<T> other) {
        return date.isBefore(other.getDate().date);
    }

    /** Check if the instance represents a time that is strictly after another.
     * @param other the instant to compare this date to
     * @return true if the instance is strictly after the argument when ordering chronologically
     * @see #isAfterOrEqualTo(FieldTimeStamped)
     * @since 10.1
     */
    public boolean isAfter(final FieldTimeStamped<T> other) {
        return date.isAfter(other.getDate().date);
    }

    /** Check if the instance represents a time that is before or equal to another.
     * @param other the instant to compare this date to
     * @return true if the instance is before (or equal to) the argument when ordering chronologically
     * @see #isBefore(FieldTimeStamped)
     * @since 10.1
     */
    public boolean isBeforeOrEqualTo(final FieldTimeStamped<T> other) {
        return date.isBeforeOrEqualTo(other.getDate().date);
    }

    /** Check if the instance represents a time that is after or equal to another.
     * @param other the instant to compare this date to
     * @return true if the instance is after (or equal to) the argument when ordering chronologically
     * @see #isAfterOrEqualTo(FieldTimeStamped)
     * @since 10.1
     */
    public boolean isAfterOrEqualTo(final FieldTimeStamped<T> other) {
        return date.isAfterOrEqualTo(other.getDate().date);
    }

    /** Check if the instance represents a time that is strictly between two others representing
     * the boundaries of a time span. The two boundaries can be provided in any order: in other words,
     * whether <code>boundary</code> represents a time that is before or after <code>otherBoundary</code> will
     * not change the result of this method.
     * @param boundary one end of the time span
     * @param otherBoundary the other end of the time span
     * @return true if the instance is strictly between the two arguments when ordering chronologically
     * @see #isBetweenOrEqualTo(FieldTimeStamped, FieldTimeStamped)
     * @since 10.1
     */
    public boolean isBetween(final FieldTimeStamped<T> boundary, final FieldTimeStamped<T> otherBoundary) {
        return date.isBetween(boundary.getDate().date, otherBoundary.getDate().date);
    }

    /** Check if the instance represents a time that is between two others representing
     * the boundaries of a time span, or equal to one of them. The two boundaries can be provided in any order:
     * in other words, whether <code>boundary</code> represents a time that is before or after
     * <code>otherBoundary</code> will not change the result of this method.
     * @param boundary one end of the time span
     * @param otherBoundary the other end of the time span
     * @return true if the instance is between the two arguments (or equal to at least one of them)
     * when ordering chronologically
     * @see #isBetween(FieldTimeStamped, FieldTimeStamped)
     * @since 10.1
     */
    public boolean isBetweenOrEqualTo(final FieldTimeStamped<T> boundary, final FieldTimeStamped<T> otherBoundary) {
        return date.isBetweenOrEqualTo(boundary.getDate().date, otherBoundary.getDate().date);
    }

    /** Get a hashcode for this date.
     * @return hashcode
     */
    public int hashCode() {
        return date.hashCode() + fieldOffset.getAddendum().hashCode();
    }

    /**
     * Get a String representation of the instant location with up to 16 digits of
     * precision for the seconds value.
     *
     * <p> Since this method is used in exception messages and error handling every
     * effort is made to return some representation of the instant. If UTC is available
     * from the default data context then it is used to format the string in UTC. If not
     * then TAI is used. Finally if the prior attempts fail this method falls back to
     * converting this class's internal representation to a string.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @return a string representation of the instance, in ISO-8601 format if UTC is
     * available from the default data context.
     * @see AbsoluteDate#toString()
     * @see #toString(TimeScale)
     * @see DateTimeComponents#toString(int, int)
     */
    @DefaultDataContext
    public String toString() {
        return date.toString();
    }

    /**
     * Get a String representation of the instant location in ISO-8601 format without the
     * UTC offset and with up to 16 digits of precision for the seconds value.
     *
     * @param timeScale time scale to use
     * @return a string representation of the instance.
     * @see DateTimeComponents#toString(int, int)
     */
    public String toString(final TimeScale timeScale) {
        return date.toString(timeScale);
    }

    /** Get a String representation of the instant location for a local time.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     * negative Westward UTC).
     * @return string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     * @see #toString(int, TimeScale)
     */
    @DefaultDataContext
    public String toString(final int minutesFromUTC) {
        return date.toString(minutesFromUTC);
    }

    /**
     * Get a String representation of the instant location for a local time.
     *
     * @param minutesFromUTC offset in <em>minutes</em> from UTC (positive Eastwards UTC,
     *                       negative Westward UTC).
     * @param utc            time scale used to compute date and time components.
     * @return string representation of the instance, in ISO-8601 format with milliseconds
     * accuracy
     * @since 10.1
     */
    public String toString(final int minutesFromUTC, final TimeScale utc) {
        return date.toString(minutesFromUTC, utc);
    }

    /** Get a String representation of the instant location for a time zone.
     *
     * <p>This method uses the {@link DataContext#getDefault() default data context}.
     *
     * @param timeZone time zone
     * @return string representation of the instance,
     * in ISO-8601 format with milliseconds accuracy
     * @see #toString(TimeZone, TimeScale)
     */
    @DefaultDataContext
    public String toString(final TimeZone timeZone) {
        return date.toString(timeZone);
    }

    /**
     * Get a String representation of the instant location for a time zone.
     *
     * @param timeZone time zone
     * @param utc      time scale used to compute date and time components.
     * @return string representation of the instance, in ISO-8601 format with milliseconds
     * accuracy
     * @since 10.1
     */
    public String toString(final TimeZone timeZone, final TimeScale utc) {
        return date.toString(timeZone, utc);
    }

    /**
     * Return a string representation of this date-time, rounded to the given precision.
     *
     * <p>The format used is ISO8601 without the UTC offset.</p>
     *
     *
     * @param timeScale      to use to compute components.
     * @param fractionDigits the number of digits to include after the decimal point in
     *                       the string representation of the seconds. The date and time
     *                       is first rounded as necessary. {@code fractionDigits} must be
     *                       greater than or equal to {@code 0}.
     * @return string representation of this date, time, and UTC offset
     * @see #toString(TimeScale)
     * @see DateTimeComponents#toString(int, int)
     * @see DateTimeComponents#toStringWithoutUtcOffset(int, int)
     * @since 12.2
     */
    public String toStringWithoutUtcOffset(final TimeScale timeScale, final int fractionDigits) {
        return date.toStringWithoutUtcOffset(timeScale, fractionDigits);
    }

    /** Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call <code>new FieldAbsoluteDate(this, dt)</code>.
     * </p>
     * @param dt time shift in seconds
     * @return a new date, shifted with respect to instance (which is immutable)
     * @see org.orekit.utils.FieldPVCoordinates#shiftedBy(double)
     * @see org.orekit.attitudes.FieldAttitude#shiftedBy(double)
     * @see org.orekit.orbits.FieldOrbit#shiftedBy(double)
     * @see org.orekit.propagation.FieldSpacecraftState#shiftedBy(double)
     */
    @Override
    public FieldAbsoluteDate<T> shiftedBy(final double dt) {
        return new FieldAbsoluteDate<>(this, dt);
    }

    /** Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call <code>new FieldAbsoluteDate(this, dt)</code>.
     * </p>
     * @param dt time shift
     * @return a new date, shifted with respect to instance (which is immutable)
     * @see org.orekit.utils.FieldPVCoordinates#shiftedBy(double)
     * @see org.orekit.attitudes.FieldAttitude#shiftedBy(double)
     * @see org.orekit.orbits.FieldOrbit#shiftedBy(double)
     * @see org.orekit.propagation.FieldSpacecraftState#shiftedBy(double)
     * @since 13.0
     */
    @Override
    public FieldAbsoluteDate<T> shiftedBy(final TimeOffset dt) {
        return new FieldAbsoluteDate<>(this, dt);
    }

    /** Get a time-shifted date.
     * <p>
     * Calling this method is equivalent to call <code>new FieldAbsoluteDate(this, dt, timeUnit)</code>.
     * </p>
     * @param dt time shift in time units
     * @param timeUnit {@link TimeUnit} for dt
     * @return a new date, shifted with respect to instance (which is immutable)
     * @see org.orekit.utils.FieldPVCoordinates#shiftedBy(double)
     * @see org.orekit.attitudes.FieldAttitude#shiftedBy(double)
     * @see org.orekit.orbits.FieldOrbit#shiftedBy(double)
     * @see org.orekit.propagation.FieldSpacecraftState#shiftedBy(double)
     * @since 12.1
     */
    public FieldAbsoluteDate<T> shiftedBy(final long dt, final TimeUnit timeUnit) {
        return new FieldAbsoluteDate<>(this, dt, timeUnit);
    }


    /** Transform the FieldAbsoluteDate in an AbsoluteDate.
     * @return AbsoluteDate of the FieldObject
     * */
    public AbsoluteDate toAbsoluteDate() {
        return date;
    }

    /** Check if the Field is semantically equal to zero.
     *
     * <p> Using {@link FieldElement#isZero()}
     *
     * @return true the Field is semantically equal to zero
     * @since 12.0
     */
    public boolean hasZeroField() {
        return fieldOffset.getAddendum().isZero();
    }

    /**
     * Return the given date as a Modified Julian Date <b>expressed in UTC</b>.
     *
     * @return double representation of the given date as Modified Julian Date.
     * @since 12.2
     */
    @DefaultDataContext
    public T getMJD() {
        return this.getMJD(TimeScalesFactory.getUTC());
    }

    /**
     * Return the given date as a Modified Julian Date expressed in given timescale.
     *
     * @param ts time scale
     * @return double representation of the given date as Modified Julian Date.
     * @since 12.2
     */
    public T getMJD(final TimeScale ts) {
        final T shift = fieldOffset.divide(Constants.JULIAN_DAY);
        return shift.add(date.getMJD(ts));
    }

    /**
     * Return the given date as a Julian Date <b>expressed in UTC</b>.
     *
     * @return double representation of the given date as Julian Date.
     * @since 12.2
     */
    @DefaultDataContext
    public T getJD() {
        return getJD(TimeScalesFactory.getUTC());
    }

    /**
     * Return the given date as a Julian Date expressed in given timescale.
     *
     * @param ts time scale
     * @return double representation of the given date as Julian Date.
     * @since 12.2
     */
    public T getJD(final TimeScale ts) {
        final T shift = fieldOffset.divide(Constants.JULIAN_DAY);
        return shift.add(date.getJD(ts));
    }

    /** Get day of year, preserving continuity as much as possible.
     * <p>
     * This is a continuous extension of the integer value returned by
     * {@link #getComponents(TimeZone) getComponents(utc)}{@link DateTimeComponents#getDate() .getDate()}{@link DateComponents#getDayOfYear() .getDayOfYear()}.
     * In order to have it remain as close as possible to its integer counterpart,
     * day 1.0 is considered to occur on January 1st at noon.
     * </p>
     * <p>
     * Continuity is preserved from day to day within a year, but of course
     * there is a discontinuity at year change, where it switches from 365.49999…
     * (or 366.49999… on leap years) to 0.5
     * </p>
     * @param utc time scale to compute date components
     * @return day of year, with day 1.0 occurring on January first at noon
     * @since 13.0
     */
    public T getDayOfYear(final TimeScale utc) {
        final int                  year         = date.getComponents(utc).getDate().getYear();
        final AbsoluteDate         newYearsEveD = new AbsoluteDate(year - 1, 12, 31, 12, 0, 0.0, utc);
        final FieldAbsoluteDate<T> newYearsEveF = new FieldAbsoluteDate<>(getField(), newYearsEveD);
        return durationFrom(newYearsEveF).divide(Constants.JULIAN_DAY);
    }

}
