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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.RegionFactory;
import org.hipparchus.geometry.spherical.twod.Circle;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.geometry.spherical.twod.SubCircle;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.geometry.fov.DoubleDihedraFieldOfView;
import org.orekit.geometry.fov.FieldOfView;
import org.orekit.geometry.fov.PolygonalFieldOfView;
import org.orekit.geometry.fov.PolygonalFieldOfView.DefiningConeType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.lang.reflect.Field;
import java.util.List;

public class FootprintOverlapDetectorTest {

    private Propagator propagator;

    private Orbit initialOrbit;

    private OneAxisEllipsoid earth;

    @Test
    public void testSampleAroundPole() throws NoSuchFieldException, IllegalAccessException {
        S2Point[] polygon = new S2Point[] {
            new S2Point(FastMath.toRadians(-120.0), FastMath.toRadians(5.0)),
            new S2Point(FastMath.toRadians(   0.0), FastMath.toRadians(5.0)),
            new S2Point(FastMath.toRadians( 120.0), FastMath.toRadians(5.0))
        };
        SphericalPolygonsSet aoi = new SphericalPolygonsSet(1.e-9, polygon);
        FieldOfView fov = new DoubleDihedraFieldOfView(Vector3D.PLUS_J,
                                                       Vector3D.PLUS_I, FastMath.toRadians(5.),
                                                       Vector3D.PLUS_K, FastMath.toRadians(5.),
                                                       0.);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, itrf);
        FootprintOverlapDetector detector = new FootprintOverlapDetector(fov, earth, aoi, 20000.);
        Assertions.assertEquals(9.91475183e-3, detector.getZone().getSize(), 1.0e-10);
        Field sampledZoneField = FootprintOverlapDetector.class.getDeclaredField("sampledZone");
        sampledZoneField.setAccessible(true);
        List<?> sampledZone = (List<?>) sampledZoneField.get(detector);
        Assertions.assertEquals(1140, sampledZone.size());
    }

    @Test
    public void testRightForwardView() {

        propagator.setAttitudeProvider(new LofOffset(initialOrbit.getFrame(), LOFType.LVLH_CCSDS,
                                                      RotationOrder.XYZ,
                                                      FastMath.toRadians(-20.0),
                                                      FastMath.toRadians(+20.0),
                                                      0.0));

        // observe continental France plus Corsica
        final SphericalPolygonsSet france = buildFrance();

        // square field of view along Z axis (which is pointing sideways), aperture 5°, 0° margin
        final FieldOfView fov = new PolygonalFieldOfView(Vector3D.PLUS_K,
                                                         DefiningConeType.INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE,
                                                         Vector3D.PLUS_I,
                                                         FastMath.toRadians(2.5), 4, 0.0);
        final FootprintOverlapDetector detector =
                new FootprintOverlapDetector(fov, earth, france, 50000.0).
                withMaxCheck(1.0).
                withThreshold(1.0e-6).
                withHandler(new ContinueOnEvent());
        final EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        // Extrapolate from the initial to the final date
        propagator.propagate(initialOrbit.getDate().shiftedBy(635000),
                             initialOrbit.getDate().shiftedBy(735000));

        List<LoggedEvent> events = logger.getLoggedEvents();
        Assertions.assertEquals(8, events.size());

        // the first two consecutive close events occur during the same ascending orbit
        // we first see Corsica, then lose visibility over the sea, then see continental France

        // above Mediterranean sea, between Illes Balears and Sardigna,
        // pointing to Corsica towards North-East
        checkEventPair(events.get(0),  events.get(1),
                       639010.0775,  34.1551, 39.2231,  6.5960, 42.0734,  9.0526);

        // above Saint-Chamond (Loire), pointing near Saint-Dié-des-Vosges (Vosges) towards North-East
        checkEventPair(events.get(2),  events.get(3),
                       639113.5532,  38.3899, 45.5356,  4.4813, 48.4211,  7.1499);

        // event is on a descending orbit, so the pointing direction,
        // taking roll and pitch offsets, is towards South-West with respect to spacecraft
        // above English Channel, pointing near Hanvec (Finistère) towards South-West
        checkEventPair(events.get(4),  events.get(5),
                       687772.4531,  27.0852, 50.2693,  0.0493, 48.3243, -4.1510);

        // event on an ascending orbit
        // above Atlantic ocean, pointing near to île d'Oléron (Charente-Maritime) towards North-East
        checkEventPair(events.get(6),  events.get(7),
                       727696.1034, 112.8867, 42.9486, -4.0325, 45.8192, -1.4565);

    }

    private void checkEventPair(final LoggedEvent start, final LoggedEvent end,
                                final double expectedStart, final double expectedDuration,
                                final double spacecraftLatitude, final double spacecraftLongitude,
                                final double fovCenterLatitude, final double fovCenterLongitude) {

        Assertions.assertFalse(start.isIncreasing());
        Assertions.assertTrue(end.isIncreasing());
        Assertions.assertEquals(expectedStart,
                            start.getState().getDate().durationFrom(initialOrbit.getDate()),
                            0.001);
        Assertions.assertEquals(expectedDuration,
                            end.getState().getDate().durationFrom(start.getState().getDate()),
                            0.001);

        SpacecraftState middle = start.getState().shiftedBy(0.5 * expectedDuration);

        // sub-satellite point
        Vector3D p = middle.getPosition();
        GeodeticPoint gpSat = earth.transform(p, middle.getFrame(), middle.getDate());
        Assertions.assertEquals(spacecraftLatitude,  FastMath.toDegrees(gpSat.getLatitude()),  0.001);
        Assertions.assertEquals(spacecraftLongitude, FastMath.toDegrees(gpSat.getLongitude()), 0.001);

        // point at center of Field Of View
        final Transform scToInert = middle.toTransform().getInverse();
        GeodeticPoint gpFOV =
                earth.getIntersectionPoint(new Line(p, scToInert.transformPosition(Vector3D.PLUS_K), 1.0e-6),
                                           middle.getPosition(),
                                           middle.getFrame(), middle.getDate());
        Assertions.assertEquals(fovCenterLatitude,  FastMath.toDegrees(gpFOV.getLatitude()),  0.001);
        Assertions.assertEquals(fovCenterLongitude, FastMath.toDegrees(gpFOV.getLongitude()), 0.001);

    }

    @BeforeEach
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            final TimeScale utc = TimeScalesFactory.getUTC();
            final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
            final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
            final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
            initialOrbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                     FramesFactory.getEME2000(), date,
                                                     Constants.EIGEN5C_EARTH_MU);

            propagator =
                new EcksteinHechlerPropagator(initialOrbit,
                                              Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                              Constants.EIGEN5C_EARTH_MU,
                                              Constants.EIGEN5C_EARTH_C20,
                                              Constants.EIGEN5C_EARTH_C30,
                                              Constants.EIGEN5C_EARTH_C40,
                                              Constants.EIGEN5C_EARTH_C50,
                                              Constants.EIGEN5C_EARTH_C60);

            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        } catch (OrekitException oe) {
            Assertions.fail(oe.getMessage());
        }

    }

    private SphericalPolygonsSet buildFrance() {

        final SphericalPolygonsSet continental = buildSimpleZone(new double[][] {
            { 51.14850,  2.51357 }, { 50.94660,  1.63900 }, { 50.12717,  1.33876 }, { 49.34737, -0.98946 },
            { 49.77634, -1.93349 }, { 48.64442, -1.61651 }, { 48.90169, -3.29581 }, { 48.68416, -4.59234 },
            { 47.95495, -4.49155 }, { 47.57032, -2.96327 }, { 46.01491, -1.19379 }, { 44.02261, -1.38422 },
            { 43.42280, -1.90135 }, { 43.03401, -1.50277 }, { 42.34338,  1.82679 }, { 42.47301,  2.98599 },
            { 43.07520,  3.10041 }, { 43.39965,  4.55696 }, { 43.12889,  6.52924 }, { 43.69384,  7.43518 },
            { 44.12790,  7.54959 }, { 45.02851,  6.74995 }, { 45.33309,  7.09665 }, { 46.42967,  6.50009 },
            { 46.27298,  6.02260 }, { 46.72577,  6.03738 }, { 47.62058,  7.46675 }, { 49.01778,  8.09927 },
            { 49.20195,  6.65822 }, { 49.44266,  5.89775 }, { 49.98537,  4.79922 }
          });

        final SphericalPolygonsSet corsica = buildSimpleZone(new double[][] {
            { 42.15249,  9.56001 }, { 43.00998,  9.39000 }, { 42.62812,  8.74600 }, { 42.25651,  8.54421 },
            { 41.58361,  8.77572 }, { 41.38000,  9.22975 }
          });

          return (SphericalPolygonsSet) new RegionFactory<Sphere2D, S2Point, Circle, SubCircle>().
                 union(continental, corsica);

    }

    private SphericalPolygonsSet buildSimpleZone(double[][] points) {
        final S2Point[] vertices = new S2Point[points.length];
        for (int i = 0; i < points.length; ++i) {
            vertices[i] = new S2Point(FastMath.toRadians(points[i][1]),         // points[i][1] is longitude
                                      FastMath.toRadians(90.0 - points[i][0])); // points[i][0] is latitude
        }
        return new SphericalPolygonsSet(1.0e-10, vertices);
    }

}
