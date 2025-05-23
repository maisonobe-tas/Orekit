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
package org.orekit.bodies;

import java.io.Serializable;
import java.text.NumberFormat;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.CompositeFormat;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;

/** Point location relative to a 2D body surface.
 * <p>Instance of this class are guaranteed to be immutable.</p>
 * @see BodyShape
 * @see FieldGeodeticPoint
 * @author Luc Maisonobe
 */
public class GeodeticPoint implements Serializable {

    /** North pole.
     * @since 10.0
     */
    public static final GeodeticPoint NORTH_POLE = new GeodeticPoint(+0.5 * FastMath.PI, 0.0, 0.0);

    /** South pole.
     * @since 10.0
     */
    public static final GeodeticPoint SOUTH_POLE = new GeodeticPoint(-0.5 * FastMath.PI, 0.0, 0.0);

    /** Serializable UID. */
    private static final long serialVersionUID = 7862466825590075399L;

    /** Latitude of the point (rad). */
    private final double latitude;

    /** Longitude of the point (rad). */
    private final double longitude;

    /** Altitude of the point (m). */
    private final double altitude;

    /** Zenith direction. */
    private transient Vector3D zenith;

    /** Nadir direction. */
    private transient Vector3D nadir;

    /** North direction. */
    private transient Vector3D north;

    /** South direction. */
    private transient Vector3D south;

    /** East direction. */
    private transient Vector3D east;

    /** West direction. */
    private transient Vector3D west;

    /**
     * Build a new instance. The angular coordinates will be normalized so that
     * the latitude is between ±π/2 and the longitude is between ±π.
     *
     * @param latitude latitude of the point (rad)
     * @param longitude longitude of the point (rad)
     * @param altitude altitude of the point (m)
     * @see SexagesimalAngle
     */
    public GeodeticPoint(final double latitude, final double longitude,
                         final double altitude) {
        double lat = MathUtils.normalizeAngle(latitude, FastMath.PI / 2);
        double lon = MathUtils.normalizeAngle(longitude, 0);
        if (lat > FastMath.PI / 2.0) {
            // latitude is beyond the pole -> add 180 to longitude
            lat = FastMath.PI - lat;
            lon = MathUtils.normalizeAngle(longitude + FastMath.PI, 0);
        }
        this.latitude  = lat;
        this.longitude = lon;
        this.altitude  = altitude;
    }

    /** Get the latitude.
     * @return latitude, an angular value in the range [-π/2, π/2]
     */
    public double getLatitude() {
        return latitude;
    }

    /** Get the longitude.
     * @return longitude, an angular value in the range [-π, π]
     */
    public double getLongitude() {
        return longitude;
    }

    /** Get the altitude.
     * @return altitude
     */
    public double getAltitude() {
        return altitude;
    }

    /** Get the direction above the point, expressed in parent shape frame.
     * <p>The zenith direction is defined as the normal to local horizontal plane.</p>
     * @return unit vector in the zenith direction
     * @see #getNadir()
     */
    public Vector3D getZenith() {
        if (zenith == null) {
            final SinCos scLat = FastMath.sinCos(latitude);
            final SinCos scLon = FastMath.sinCos(longitude);
            zenith = new Vector3D(scLon.cos() * scLat.cos(), scLon.sin() * scLat.cos(), scLat.sin());
        }
        return zenith;
    }

    /** Get the direction below the point, expressed in parent shape frame.
     * <p>The nadir direction is the opposite of zenith direction.</p>
     * @return unit vector in the nadir direction
     * @see #getZenith()
     */
    public Vector3D getNadir() {
        if (nadir == null) {
            nadir = getZenith().negate();
        }
        return nadir;
    }

    /** Get the direction to the north of point, expressed in parent shape frame.
     * <p>The north direction is defined in the horizontal plane
     * (normal to zenith direction) and following the local meridian.</p>
     * @return unit vector in the north direction
     * @see #getSouth()
     */
    public Vector3D getNorth() {
        if (north == null) {
            final SinCos scLat = FastMath.sinCos(latitude);
            final SinCos scLon = FastMath.sinCos(longitude);
            north = new Vector3D(-scLon.cos() * scLat.sin(), -scLon.sin() * scLat.sin(), scLat.cos());
        }
        return north;
    }

    /** Get the direction to the south of point, expressed in parent shape frame.
     * <p>The south direction is the opposite of north direction.</p>
     * @return unit vector in the south direction
     * @see #getNorth()
     */
    public Vector3D getSouth() {
        if (south == null) {
            south = getNorth().negate();
        }
        return south;
    }

    /** Get the direction to the east of point, expressed in parent shape frame.
     * <p>The east direction is defined in the horizontal plane
     * in order to complete direct triangle (east, north, zenith).</p>
     * @return unit vector in the east direction
     * @see #getWest()
     */
    public Vector3D getEast() {
        if (east == null) {
            final SinCos scLon = FastMath.sinCos(longitude);
            east = new Vector3D(-scLon.sin(), scLon.cos(), 0);
        }
        return east;
    }

    /** Get the direction to the west of point, expressed in parent shape frame.
     * <p>The west direction is the opposite of east direction.</p>
     * @return unit vector in the west direction
     * @see #getEast()
     */
    public Vector3D getWest() {
        if (west == null) {
            west = getEast().negate();
        }
        return west;
    }

    @Override
    public boolean equals(final Object object) {
        if (object instanceof GeodeticPoint) {
            final GeodeticPoint other = (GeodeticPoint) object;
            return this.getLatitude() == other.getLatitude() &&
                   this.getLongitude() == other.getLongitude() &&
                   this.getAltitude() == other.getAltitude();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Double.valueOf(this.getLatitude()).hashCode() ^
               Double.valueOf(this.getLongitude()).hashCode() ^
               Double.valueOf(this.getAltitude()).hashCode();
    }

    @Override
    public String toString() {
        final NumberFormat format = CompositeFormat.getDefaultNumberFormat();
        return "{lat: " +
               format.format(FastMath.toDegrees(this.getLatitude())) +
               " deg, lon: " +
               format.format(FastMath.toDegrees(this.getLongitude())) +
               " deg, alt: " +
               format.format(this.getAltitude()) +
               "}";
    }
}
