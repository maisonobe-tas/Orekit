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
package org.orekit.gnss.metric.messages.rtcm;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataContext;
import org.orekit.gnss.metric.messages.rtcm.correction.Rtcm1064;
import org.orekit.gnss.metric.messages.rtcm.correction.RtcmClockCorrectionData;
import org.orekit.gnss.metric.parser.ByteArrayEncodedMessage;
import org.orekit.gnss.metric.parser.EncodedMessage;
import org.orekit.gnss.metric.parser.RtcmMessagesParser;

public class Rtcm1064Test {

    private double eps = 1.0e-13;

    private EncodedMessage message;

    private ArrayList<Integer> messages;

    @BeforeEach
    public void setUp() {

        final String m = "010000101000" +                      // Message number
                         "01111110011000111" +                 // GLONASS Epoch Time 1s
                         "0101" +                              // SSR Update Interval
                         "0" +                                 // Multiple Message Indicator
                         "0111" +                              // IOD SSR
                         "0000111101101111" +                  // SSR Provider ID
                         "0001" +                              // SSR Solution ID
                         "000001" +                            // No. of Satellites
                         "00001" +                             // Satellite ID
                         "0011101011111101111111" +            // Delta Clock C0
                         "001110101111110111111" +             // Delta Clock C1
                         "0011101011111101111111000110000000"; // Delta Clock C2

        message = new ByteArrayEncodedMessage(byteArrayFromBinary(m));
        message.start();

        messages = new ArrayList<>();
        messages.add(1064);

    }

    @Test
    public void testPerfectValue() {
        final Rtcm1064 rtcm1064 = (Rtcm1064) new RtcmMessagesParser(messages, DataContext.getDefault().getTimeScales()).
                                  parse(message, false);

        // Verify size
        Assertions.assertEquals(1,                            rtcm1064.getData().size());

        // Verify header
        Assertions.assertEquals(1064,                         rtcm1064.getTypeCode());
        Assertions.assertEquals(64711.0,                      rtcm1064.getHeader().getEpochTime1s(), eps);
        Assertions.assertEquals(30.0,                         rtcm1064.getHeader().getSsrUpdateInterval().getUpdateInterval(), eps);
        Assertions.assertEquals(0,                            rtcm1064.getHeader().getMultipleMessageIndicator());
        Assertions.assertEquals(7,                            rtcm1064.getHeader().getIodSsr());
        Assertions.assertEquals(3951,                         rtcm1064.getHeader().getSsrProviderId());
        Assertions.assertEquals(1,                            rtcm1064.getHeader().getSsrSolutionId());
        Assertions.assertEquals(1,                            rtcm1064.getHeader().getNumberOfSatellites());

        // Verify data for satellite R01
        final RtcmClockCorrectionData r01 = rtcm1064.getDataMap().get("R01").get(0);
        Assertions.assertEquals(1,                            r01.getSatelliteID());
        Assertions.assertEquals(96.6527,                      r01.getClockCorrection().getDeltaClockC0(),            eps);
        Assertions.assertEquals(0.483263,                     r01.getClockCorrection().getDeltaClockC1(),            eps);
        Assertions.assertEquals(0.61857734,                   r01.getClockCorrection().getDeltaClockC2(),            eps);
    }

    private byte[] byteArrayFromBinary(String radix2Value) {
        final byte[] array = new byte[radix2Value.length() / 8];
        for (int i = 0; i < array.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                if (radix2Value.charAt(8 * i + j) != '0') {
                    array[i] |= 0x1 << (7 - j);
                }
            }
        }
        return array;
    }

}
