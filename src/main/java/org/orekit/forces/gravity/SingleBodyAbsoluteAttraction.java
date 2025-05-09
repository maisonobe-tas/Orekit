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
package org.orekit.forces.gravity;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBodies;
import org.orekit.bodies.CelestialBody;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ExtendedPositionProvider;

/** Body attraction force model computed as absolute acceleration towards a body.
 * <p>
 * This force model represents the same physical principles as {@link NewtonianAttraction},
 * but has several major differences:
 * </p>
 * <ul>
 *   <li>the attracting body can be <em>away</em> from the integration frame center,</li>
 *   <li>several instances of this force model can be added when several bodies are involved,</li>
 *   <li>this force model is <em>never</em> automatically added by the numerical propagator</li>
 * </ul>
 * <p>
 * The possibility for the attracting body to be away from the frame center allows to use this force
 * model when integrating for example an interplanetary trajectory propagated in an Earth centered
 * frame (in which case an instance of {@link org.orekit.forces.inertia.InertialForces} must also be
 * added to take into account the coupling effect of relative frames motion).
 * </p>
 * <p>
 * The possibility to add several instances allows to use this in interplanetary trajectories or
 * in trajectories about Lagrangian points
 * </p>
 * <p>
 * The fact this force model is <em>never</em> automatically added by the numerical propagator differs
 * from {@link NewtonianAttraction} as {@link NewtonianAttraction} may be added automatically when
 * propagating a trajectory represented as an {@link org.orekit.orbits.Orbit}, which must always refer
 * to a central body, if user did not add the {@link NewtonianAttraction} or set the central attraction
 * coefficient by himself.
 * </p>
 * @see org.orekit.forces.inertia.InertialForces
 * @author Luc Maisonobe
 * @author Julio Hernanz
 */
public class SingleBodyAbsoluteAttraction extends AbstractBodyAttraction {

    /** Simple constructor.
     * @param positionProvider extended position provider for the body to consider
     * @param name name of the body
     * @param mu body gravitational constant
     * @since 13.0
     */
    public SingleBodyAbsoluteAttraction(final ExtendedPositionProvider positionProvider,
                                        final String name, final double mu) {
        super(positionProvider, name, mu);
    }

    /** Constructor.
     * @param body the body to consider
     * (ex: {@link CelestialBodies#getSun()} or
     * {@link CelestialBodies#getMoon()})
     */
    public SingleBodyAbsoluteAttraction(final CelestialBody body) {
        this(body, body.getName(), body.getGM());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        // compute bodies separation vectors and squared norm
        final Vector3D bodyPosition = getBodyPosition(s.getDate(), s.getFrame());
        final Vector3D satToBody     = bodyPosition.subtract(s.getPosition());
        final double r2Sat           = satToBody.getNormSq();

        // compute absolute acceleration
        return new Vector3D(parameters[0] / (r2Sat * FastMath.sqrt(r2Sat)), satToBody);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters) {
         // compute bodies separation vectors and squared norm
        final FieldVector3D<T> centralToBody = getBodyPosition(s.getDate(), s.getFrame());
        final FieldVector3D<T> satToBody     = centralToBody.subtract(s.getPosition());
        final T                r2Sat         = satToBody.getNormSq();

        // compute absolute acceleration
        return new FieldVector3D<>(parameters[0].divide(r2Sat.multiply(r2Sat.sqrt())), satToBody);

    }

}
