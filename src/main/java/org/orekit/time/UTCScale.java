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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;

/** Coordinated Universal Time.
 * <p>UTC is related to TAI using step adjustments from time to time
 * according to IERS (International Earth Rotation Service) rules. Before 1972,
 * these adjustments were piecewise linear offsets. Since 1972, these adjustments
 * are piecewise constant offsets, which require introduction of leap seconds.</p>
 * <p>Leap seconds are always inserted as additional seconds at the last minute
 * of the day, pushing the next day forward. Such minutes are therefore more
 * than 60 seconds long. In theory, there may be seconds removal instead of seconds
 * insertion, but up to now (2010) it has never been used. As an example, when a
 * one second leap was introduced at the end of 2005, the UTC time sequence was
 * 2005-12-31T23:59:59 UTC, followed by 2005-12-31T23:59:60 UTC, followed by
 * 2006-01-01T00:00:00 UTC.</p>
 * <p>This is intended to be accessed thanks to {@link TimeScales},
 * so there is no public constructor.</p>
 * @author Luc Maisonobe
 * @see AbsoluteDate
 */
public class UTCScale implements TimeScale {

    /** Number of seconds in one day. */
    private static final long SEC_PER_DAY = 86400L;

    /** Number of attoseconds in one second. */
    private static final long ATTOS_PER_NANO = 1000000000L;

    /** Slope conversion factor from seconds per day to nanoseconds per second. */
    private static final long SLOPE_FACTOR = SEC_PER_DAY * ATTOS_PER_NANO;

    /** International Atomic Scale. */
    private final TimeScale tai;

    /** base UTC-TAI offsets (may lack the pre-1975 offsets). */
    private final Collection<? extends OffsetModel> baseOffsets;

    /** UTC-TAI offsets. */
    private final UTCTAIOffset[] offsets;

    /** Package private constructor for the factory.
     * Used to create the prototype instance of this class that is used to
     * clone all subsequent instances of {@link UTCScale}. Initializes the offset
     * table that is shared among all instances.
     * @param tai TAI time scale this UTC time scale references.
     * @param baseOffsets UTC-TAI base offsets (may lack the pre-1975 offsets)
     */
    UTCScale(final TimeScale tai, final Collection<? extends OffsetModel> baseOffsets) {

        this.tai         = tai;
        this.baseOffsets = baseOffsets;

        // copy input so the original list is unmodified
        final List<OffsetModel> offsetModels = new ArrayList<>(baseOffsets);
        offsetModels.sort(Comparator.comparing(OffsetModel::getStart));
        if (offsetModels.get(0).getStart().getYear() > 1968) {
            // the pre-1972 linear offsets are missing, add them manually
            // excerpt from UTC-TAI.history file:
            //  1961  Jan.  1 - 1961  Aug.  1     1.422 818 0s + (MJD - 37 300) x 0.001 296s
            //        Aug.  1 - 1962  Jan.  1     1.372 818 0s +        ""
            //  1962  Jan.  1 - 1963  Nov.  1     1.845 858 0s + (MJD - 37 665) x 0.001 123 2s
            //  1963  Nov.  1 - 1964  Jan.  1     1.945 858 0s +        ""
            //  1964  Jan.  1 -       April 1     3.240 130 0s + (MJD - 38 761) x 0.001 296s
            //        April 1 -       Sept. 1     3.340 130 0s +        ""
            //        Sept. 1 - 1965  Jan.  1     3.440 130 0s +        ""
            //  1965  Jan.  1 -       March 1     3.540 130 0s +        ""
            //        March 1 -       Jul.  1     3.640 130 0s +        ""
            //        Jul.  1 -       Sept. 1     3.740 130 0s +        ""
            //        Sept. 1 - 1966  Jan.  1     3.840 130 0s +        ""
            //  1966  Jan.  1 - 1968  Feb.  1     4.313 170 0s + (MJD - 39 126) x 0.002 592s
            //  1968  Feb.  1 - 1972  Jan.  1     4.213 170 0s +        ""
            // the slopes in second per day correspond in fact to values in scaled nanoseconds per seconds:
            //  0.0012960 s/d → 15 ns/s
            //  0.0011232 s/d → 13 ns/s
            //  0.0025920 s/d → 30 ns/s
            // CHECKSTYLE: stop MultipleStringLiterals check
            offsetModels.add( 0, linearModel(1961,  1, 1, 37300, "1.4228180", "0.001296"));
            offsetModels.add( 1, linearModel(1961,  8, 1, 37300, "1.3728180", "0.001296"));
            offsetModels.add( 2, linearModel(1962,  1, 1, 37665, "1.8458580", "0.0011232"));
            offsetModels.add( 3, linearModel(1963, 11, 1, 37665, "1.9458580", "0.0011232"));
            offsetModels.add( 4, linearModel(1964,  1, 1, 38761, "3.2401300", "0.001296"));
            offsetModels.add( 5, linearModel(1964,  4, 1, 38761, "3.3401300", "0.001296"));
            offsetModels.add( 6, linearModel(1964,  9, 1, 38761, "3.4401300", "0.001296"));
            offsetModels.add( 7, linearModel(1965,  1, 1, 38761, "3.5401300", "0.001296"));
            offsetModels.add( 8, linearModel(1965,  3, 1, 38761, "3.6401300", "0.001296"));
            offsetModels.add( 9, linearModel(1965,  7, 1, 38761, "3.7401300", "0.001296"));
            offsetModels.add(10, linearModel(1965,  9, 1, 38761, "3.8401300", "0.001296"));
            offsetModels.add(11, linearModel(1966,  1, 1, 39126, "4.3131700", "0.002592"));
            offsetModels.add(12, linearModel(1968,  2, 1, 39126, "4.2131700", "0.002592"));
            // CHECKSTYLE: resume MultipleStringLiterals check
        }

        // create cache
        this.offsets = new UTCTAIOffset[offsetModels.size()];

        UTCTAIOffset previous = null;

        // link the offsets together
        for (int i = 0; i < offsetModels.size(); ++i) {

            final OffsetModel    o      = offsetModels.get(i);
            final DateComponents date   = o.getStart();
            final int            mjdRef = o.getMJDRef();
            final TimeOffset offset = o.getOffset();
            final int            slope  = o.getSlope();

            // start of the leap
            final TimeOffset previousOffset = (previous == null) ?
                                              TimeOffset.ZERO :
                                              previous.getOffset(date, TimeComponents.H00);
            final AbsoluteDate leapStart   = new AbsoluteDate(date, tai).shiftedBy(previousOffset);

            // end of the leap
            final long         dt          = (date.getMJD() - mjdRef) * SEC_PER_DAY;
            final TimeOffset drift       = TimeOffset.NANOSECOND.multiply(slope * FastMath.abs(dt));
            final TimeOffset startOffset = dt < 0 ? offset.subtract(drift) : offset.add(drift);
            final AbsoluteDate leapEnd     = new AbsoluteDate(date, tai).shiftedBy(startOffset);

            // leap computed at leap start and in UTC scale
            final TimeOffset leap           = leapEnd.accurateDurationFrom(leapStart).
                                             multiply(1000000000).
                                             divide(1000000000 + slope);

            final AbsoluteDate reference = AbsoluteDate.createMJDDate(mjdRef, 0, tai).shiftedBy(offset);
            previous = new UTCTAIOffset(leapStart, date.getMJD(), leap, offset, mjdRef, slope, reference);
            this.offsets[i] = previous;

        }

    }

    /** Get the base offsets.
     * @return base offsets (may lack the pre-1975 offsets)
     * @since 12.0
     */
    public Collection<? extends OffsetModel> getBaseOffsets() {
        return baseOffsets;
    }

    /**
     * Returns the UTC-TAI offsets underlying this UTC scale.
     * <p>
     * Modifications to the returned list will not affect this UTC scale instance.
     * @return new non-null modifiable list of UTC-TAI offsets time-sorted from
     *         earliest to latest
     */
    public List<UTCTAIOffset> getUTCTAIOffsets() {
        return Arrays.asList(offsets);
    }

    /** {@inheritDoc} */
    @Override
    public TimeOffset offsetFromTAI(final AbsoluteDate date) {
        final int offsetIndex = findOffsetIndex(date);
        if (offsetIndex < 0) {
            // the date is before the first known leap
            return TimeOffset.ZERO;
        } else {
            return offsets[offsetIndex].getOffset(date).negate();
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T offsetFromTAI(final FieldAbsoluteDate<T> date) {
        final int offsetIndex = findOffsetIndex(date.toAbsoluteDate());
        if (offsetIndex < 0) {
            // the date is before the first known leap
            return date.getField().getZero();
        } else {
            return offsets[offsetIndex].getOffset(date).negate();
        }
    }

    /** {@inheritDoc} */
    @Override
    public TimeOffset offsetToTAI(final DateComponents date,
                                  final TimeComponents time) {

        // take offset from local time into account, but ignoring seconds,
        // so when we parse an hour like 23:59:60.5 during leap seconds introduction,
        // we do not jump to next day
        final int minuteInDay = time.getHour() * 60 + time.getMinute() - time.getMinutesFromUTC();
        final int correction  = minuteInDay < 0 ? (minuteInDay - 1439) / 1440 : minuteInDay / 1440;

        // find close neighbors, assuming date in TAI, i.e a date earlier than real UTC date
        final int mjd = date.getMJD() + correction;
        final UTCTAIOffset offset = findOffset(mjd);
        if (offset == null) {
            // the date is before the first known leap
            return TimeOffset.ZERO;
        } else {
            return offset.getOffset(date, time);
        }

    }

    /** {@inheritDoc} */
    public String getName() {
        return "UTC";
    }

    /** {@inheritDoc} */
    public String toString() {
        return getName();
    }

    /** Get the date of the first known leap second.
     * @return date of the first known leap second
     */
    public AbsoluteDate getFirstKnownLeapSecond() {
        return offsets[0].getDate();
    }

    /** Get the date of the last known leap second.
     * @return date of the last known leap second
     */
    public AbsoluteDate getLastKnownLeapSecond() {
        return offsets[offsets.length - 1].getDate();
    }

    /** {@inheritDoc} */
    @Override
    public boolean insideLeap(final AbsoluteDate date) {
        final int offsetIndex = findOffsetIndex(date);
        if (offsetIndex < 0) {
            // the date is before the first known leap
            return false;
        } else {
            return date.compareTo(offsets[offsetIndex].getValidityStart()) < 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> boolean insideLeap(final FieldAbsoluteDate<T> date) {
        return insideLeap(date.toAbsoluteDate());
    }

    /** {@inheritDoc} */
    @Override
    public int minuteDuration(final AbsoluteDate date) {
        final int offsetIndex = findOffsetIndex(date);
        final UTCTAIOffset offset;
        if (offsetIndex >= 0 &&
                date.compareTo(offsets[offsetIndex].getValidityStart()) < 0) {
            // the date is during the leap itself
            offset = offsets[offsetIndex];
        } else if (offsetIndex + 1 < offsets.length &&
            offsets[offsetIndex + 1].getDate().durationFrom(date) <= 60.0) {
            // the date is after a leap, but it may be just before the next one
            // the next leap will start in one minute, it will extend the current minute
            offset = offsets[offsetIndex + 1];
        } else {
            offset = null;
        }
        if (offset != null) {
            // since this method returns an int we can't return the precise duration in
            // all cases, but we can bound it. Some leaps are more than 1s. See #694
            return 60 + (int) (offset.getLeap().getSeconds() +
                               FastMath.min(1, offset.getLeap().getAttoSeconds()));
        }
        // no leap is expected within the next minute
        return 60;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> int minuteDuration(final FieldAbsoluteDate<T> date) {
        return minuteDuration(date.toAbsoluteDate());
    }

    /** {@inheritDoc} */
    @Override
    public TimeOffset getLeap(final AbsoluteDate date) {
        final int offsetIndex = findOffsetIndex(date);
        if (offsetIndex < 0) {
            // the date is before the first known leap
            return TimeOffset.ZERO;
        } else {
            return offsets[offsetIndex].getLeap();
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T getLeap(final FieldAbsoluteDate<T> date) {
        return date.getField().getZero().newInstance(getLeap(date.toAbsoluteDate()).toDouble());
    }

    /** Find the index of the offset valid at some date.
     * @param date date at which offset is requested
     * @return index of the offset valid at this date, or -1 if date is before first offset.
     */
    private int findOffsetIndex(final AbsoluteDate date) {
        int inf = 0;
        int sup = offsets.length;
        while (sup - inf > 1) {
            final int middle = (inf + sup) >>> 1;
            if (date.compareTo(offsets[middle].getDate()) < 0) {
                sup = middle;
            } else {
                inf = middle;
            }
        }
        if (sup == offsets.length) {
            // the date is after the last known leap second
            return offsets.length - 1;
        } else if (date.compareTo(offsets[inf].getDate()) < 0) {
            // the date is before the first known leap
            return -1;
        } else {
            return inf;
        }
    }

    /** Find the offset valid at some date.
     * @param mjd Modified Julian Day of the date at which offset is requested
     * @return offset valid at this date, or null if date is before first offset.
     */
    private UTCTAIOffset findOffset(final int mjd) {
        int inf = 0;
        int sup = offsets.length;
        while (sup - inf > 1) {
            final int middle = (inf + sup) >>> 1;
            if (mjd < offsets[middle].getMJD()) {
                sup = middle;
            } else {
                inf = middle;
            }
        }
        if (sup == offsets.length) {
            // the date is after the last known leap second
            return offsets[offsets.length - 1];
        } else if (mjd < offsets[inf].getMJD()) {
            // the date is before the first known leap
            return null;
        } else {
            return offsets[inf];
        }
    }

    /** Create a linear model.
     * @param year year
     * @param month month
     * @param day day
     * @param mjdRef reference date for the linear model
     * @param offset offset
     * @param slope slope
     * @return linear model
     */
    private OffsetModel linearModel(final int year, final int month, final int day,
                                    final int mjdRef, final String offset, final String slope) {
        return new OffsetModel(new DateComponents(year, month, day),
                               mjdRef,
                               TimeOffset.parse(offset),
                               (int) (TimeOffset.parse(slope).getAttoSeconds()  / SLOPE_FACTOR));
    }

}
