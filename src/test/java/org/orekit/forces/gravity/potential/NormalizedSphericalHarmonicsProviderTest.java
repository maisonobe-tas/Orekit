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
package org.orekit.forces.gravity.potential;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;

class NormalizedSphericalHarmonicsProviderTest {

    @Test
    void testGetNormalizedC20() {
        // GIVEN
        final double expectedC20 = 1.;
        final TestNormalizedProvider testNormalizedProvider = new TestNormalizedProvider(expectedC20);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final double actualC20 = testNormalizedProvider.getNormalizedC20(date);
        // THEN
        Assertions.assertEquals(expectedC20, actualC20, 0.);
    }

    private static class TestNormalizedProvider implements NormalizedSphericalHarmonicsProvider {

        private final double c20;

        TestNormalizedProvider(final double c20) {
            this.c20 = c20;
        }

        @Override
        public int getMaxDegree() {
            return 2;
        }

        @Override
        public int getMaxOrder() {
            return 0;
        }

        @Override
        public double getMu() {
            return 0;
        }

        @Override
        public double getAe() {
            return 0;
        }

        @Override
        public AbsoluteDate getReferenceDate() {
            return null;
        }

        @Override
        public TideSystem getTideSystem() {
            return null;
        }

        @Override
        public NormalizedSphericalHarmonics onDate(AbsoluteDate date) {
            return new NormalizedSphericalHarmonics() {
                @Override
                public double getNormalizedCnm(int n, int m) {
                    if ((n == 2) && (m == 0)) {
                        return c20;
                    }
                    return 0;
                }

                @Override
                public double getNormalizedSnm(int n, int m) {
                    return 0;
                }

                @Override
                public AbsoluteDate getDate() {
                    return null;
                }
            };
        }
    }

}
