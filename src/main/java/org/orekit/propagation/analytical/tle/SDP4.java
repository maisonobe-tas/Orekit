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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.utils.Constants;

/** This class contains methods to compute propagated coordinates with the SDP4 model.
 * <p>
 * The user should not bother in this class since it is handled internally by the
 * {@link TLEPropagator}.
 * </p>
 * <p>This implementation is largely inspired from the paper and source code <a
 * href="https://www.celestrak.com/publications/AIAA/2006-6753/">Revisiting Spacetrack
 * Report #3</a> and is fully compliant with its results and tests cases.</p>
 * @author Felix R. Hoots, Ronald L. Roehrich, December 1980 (original fortran)
 * @author David A. Vallado, Paul Crawford, Richard Hujsak, T.S. Kelso (C++ translation and improvements)
 * @author Fabien Maussion (java translation)
 */
abstract class SDP4  extends TLEPropagator {

    // CHECKSTYLE: stop VisibilityModifier check

    /** New perigee argument. */
    protected double omgadf;

    /** New mean motion. */
    protected double xn;

    /** Parameter for xl computation. */
    protected double xll;

    /** New eccentricity. */
    protected double em;

    /** New inclination. */
    protected double xinc;

    // CHECKSTYLE: resume VisibilityModifier check

    /** Constructor for a unique initial TLE.
     * @param initialTLE the TLE to propagate.
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @param teme the TEME frame to use for propagation.
     */
    protected SDP4(final TLE initialTLE,
                   final AttitudeProvider attitudeProvider,
                   final double mass,
                   final Frame teme) {
        super(initialTLE, attitudeProvider, mass, teme);
    }

    /** Initialization proper to each propagator (SGP or SDP).
     */
    protected void sxpInitialize() {
        luniSolarTermsComputation();
    }  // End of initialization

    /** Propagation proper to each propagator (SGP or SDP).
     * @param tSince the offset from initial epoch (minutes)
     */
    protected void sxpPropagate(final double tSince) {

        // Update for secular gravity and atmospheric drag
        omgadf = tle.getPerigeeArgument() + omgdot * tSince;
        final double xnoddf = tle.getRaan() + xnodot * tSince;
        final double tSinceSq = tSince * tSince;
        xnode = xnoddf + xnodcf * tSinceSq;
        xn = xn0dp;

        // Update for deep-space secular effects
        xll = tle.getMeanAnomaly() + xmdot * tSince;

        deepSecularEffects(tSince);

        final double tempa = 1 - c1 * tSince;
        a   = FastMath.pow(TLEConstants.XKE / xn, TLEConstants.TWO_THIRD) * tempa * tempa;
        em -= tle.getBStar(tle.getDate().shiftedBy(tSince)) * c4 * tSince;

        // Update for deep-space periodic effects
        xll += xn0dp * t2cof * tSinceSq;

        deepPeriodicEffects(tSince);

        xl = xll + omgadf + xnode;

        // Dundee change:  Reset cosio,  sinio for new xinc:
        final SinCos scI0 = FastMath.sinCos(xinc);
        cosi0 = scI0.cos();
        sini0 = scI0.sin();
        e = em;
        i = xinc;
        omega = omgadf;
        // end of calculus, go for PV computation
    }

    /** Computes SPACETRACK#3 compliant earth rotation angle.
     * @param date the current date
     * @return the ERA (rad)
     */
    protected double thetaG(final AbsoluteDate date) {

        // Reference:  The 1992 Astronomical Almanac, page B6.
        final double omega_E = 1.00273790934;
        final double jd = date
                .getComponents(utc)
                .offsetFrom(DateTimeComponents.JULIAN_EPOCH) /
                Constants.JULIAN_DAY;

        // Earth rotations per sidereal day (non-constant)
        final double UT = (jd + 0.5) % 1;
        final double seconds_per_day = Constants.JULIAN_DAY;
        final double jd_2000 = 2451545.0;   /* 1.5 Jan 2000 = JD 2451545. */
        final double t_cen = (jd - UT - jd_2000) / 36525.;
        double GMST = 24110.54841 +
                      t_cen * (8640184.812866 + t_cen * (0.093104 - t_cen * 6.2E-6));
        GMST = (GMST + seconds_per_day * omega_E * UT) % seconds_per_day;
        if (GMST < 0.) {
            GMST += seconds_per_day;
        }

        return MathUtils.TWO_PI * GMST / seconds_per_day;

    }

    /** Computes luni - solar terms from initial coordinates and epoch.
     */
    protected abstract void luniSolarTermsComputation();

    /** Computes secular terms from current coordinates and epoch.
     * @param t offset from initial epoch (min)
     */
    protected abstract void deepSecularEffects(double t);

    /** Computes periodic terms from current coordinates and epoch.
     * @param t offset from initial epoch (min)
     */
    protected abstract void deepPeriodicEffects(double t);

}
