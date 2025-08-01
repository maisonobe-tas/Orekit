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
package org.orekit.propagation;

import org.hipparchus.CalculusFieldElement;
import org.orekit.time.FieldAbsoluteDate;

/** This interface allows to modify {@link FieldSpacecraftState} and set up additional data.
 * <p>
 * {@link FieldPropagator Propagators} generate {@link FieldSpacecraftState states} that contain at
 * least orbit, attitude, and mass. These states may however also contain {@link
 * FieldSpacecraftState#addAdditionalData(String, Object)}  additional data}.
 * Instances of classes implementing this interface are intended to be registered to propagators
 * so they can either modify the basic components (orbit, attitude and mass) or add additional
 * data incrementally after having computed the basic components.
 * </p>
 * <p>
 * Some additional data may depend on previous additional data to
 * be already available the before they can be computed. It may even be impossible to compute some
 * of these additional data at some time if they depend on conditions that are fulfilled only
 * after propagation as started or some event has occurred. As the propagator builds the complete
 * state incrementally, looping over the registered providers, it must call their {@link
 * #update(FieldSpacecraftState) update} methods in an order that fulfill these dependencies that
 * may be time-dependent and are not related to the order in which the providers are registered to
 * the propagator. This reordering is performed each time the complete state is built, using a yield
 * mechanism. The propagator first pushes all providers in a stack and then empty the stack, one provider
 * at a time, taking care to select only providers that do <em>not</em> {@link
 * #yields(FieldSpacecraftState) yield} when asked. Consider for example a case where providers A, B and C
 * have been registered and provider B needs in fact the additional data generated by provider C. Then
 * when a complete state is built, the propagator puts the three providers in a new stack, and then starts the incremental
 * generation of additional data. It first checks provider A which does not yield so it is popped from
 * the stack and the additional data it generates is added. Then provider B is checked, but it yields
 * because data from provider C is not yet available. So propagator checks provider C which does not
 * yield, so it is popped out of the stack and applied. At this stage, provider B is the only remaining one
 * in the stack, so it is checked again, but this time it does not yield because the data from provider
 * C is available as it has just been added, so provider B is popped from the stack and applied. The stack
 * is now empty and the propagator can return the completed state.
 * </p>
 * <p>
 * It is possible that at some stages in the propagation, a subset of the providers registered to a
 * propagator all yield and cannot {@link #update(FieldSpacecraftState) update} the data.
 * This happens for example during the initialization phase of a propagator that
 * computes State Transition Matrices or Jacobian matrices. These features are managed as secondary equations
 * in the ODE integrator, and initialized after the primary equations (which correspond to orbit) have
 * been initialized. So when the primary equation are initialized, the providers that depend on the secondary
 * data will all yield. This behavior is expected. Another case occurs when users set up additional data
 * that induce a dependency loop (data A depending on data B which depends on data C which depends on
 * data A). In this case, the three corresponding providers will wait for each other and indefinitely yield.
 * This second case is a deadlock and results from a design error of the additional data management at
 * application level. The propagator cannot know it in advance if a subset of providers that all yield is
 * normal or not. So at propagator level, when either situation is detected, the propagator just gives up and
 * returns the most complete state it was able to compute, without generating any error. Errors will indeed
 * not be triggered in the first case (once the primary equations have been initialized, the secondary
 * equations will be initialized too), and they will be triggered in the second case as soon as user attempts
 * to retrieve an additional data that was not added.
 * </p>
 * @see org.orekit.propagation.FieldPropagator
 * @see org.orekit.propagation.integration.FieldAdditionalDerivativesProvider
 * @see FieldAbstractStateModifier
 * @author Luc Maisonobe
 * @param <O> type of the additional data
 * @param <T> type of the field elements
 * @since 13.0
 */
public interface FieldAdditionalDataProvider<O, T extends CalculusFieldElement<T>> {

    /** Get the name of the additional data.
     * <p>
     * If a provider just modifies one of the basic elements (orbit, attitude
     * or mass) without adding any new data, it should return the empty string
     * as its name.
     * </p>
     * @return name of the additional data (names containing "orekit"
     * with any case are reserved for the library internal use)
     */
    String getName();

    /** Initialize the additional state provider at the start of propagation.
     * @param initialState initial state information at the start of propagation
     * @param target       date of propagation
     * @since 11.2
     */
    default void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
        // nothing by default
    }

    /** Check if this provider should yield so another provider has an opportunity to add missing parts.
     * <p>
     * Decision to yield is often based on an additional data being {@link FieldSpacecraftState#hasAdditionalData(String)
     * already available} in the provided {@code state} (but it could theoretically also depend on
     * an additional state derivative being {@link FieldSpacecraftState#hasAdditionalStateDerivative(String)
     * already available}, or any other criterion). If for example a provider needs the state transition
     * matrix, it could implement this method as:
     * </p>
     * <pre>{@code
     * public boolean yields(final FieldSpacecraftState state) {
     *     return state.hasAdditionalData("STM");
     * }
     * }</pre>
     * <p>
     * The default implementation returns {@code false}, meaning that the data can be
     * {@link #getAdditionalData(FieldSpacecraftState) generated} immediately.
     * </p>
     * @param state state to handle
     * @return true if this provider should yield so another provider has an opportunity to add missing parts
     * as the state is incrementally built up
     * @since 11.1
     */
    default boolean yields(final FieldSpacecraftState<T> state) {
        return false;
    }

    /** Get the additional data.
     * @param state spacecraft state to which additional data should correspond
     * @return additional data corresponding to spacecraft state
     */
    O getAdditionalData(FieldSpacecraftState<T> state);

    /** Update a state.
     * @param state spacecraft state to update
     * @return updated state
     * @since 12.1
     */
    default FieldSpacecraftState<T> update(final FieldSpacecraftState<T> state) {
        return state.addAdditionalData(getName(), getAdditionalData(state));
    }

}
