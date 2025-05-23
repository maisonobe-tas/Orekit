/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.events.handlers;

import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;

/**
 * Event handler counting event occurrences and always returning {@link Action#CONTINUE}.
 * @see CountingHandler
 * @author Romain Serra
 * @since 13.0
 */
public class CountAndContinue extends CountingHandler {

    /** Constructor with count initialized at zero.
     */
    public CountAndContinue() {
        this(0);
    }

    /** Constructor.
     * @param startingCount value to initialize count
     */
    public CountAndContinue(final int startingCount) {
        super(startingCount, Action.CONTINUE);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doesCount(final SpacecraftState state, final EventDetector detector, final boolean increasing) {
        return true;
    }
}
