/* Copyright 2022-2025 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.section.CommentsContainer;

/** Attitude determination sensor data.
 * <p>
 * Beware that the Orekit getters and setters all rely on SI units. The parsers
 * and writers take care of converting these SI units into CCSDS mandatory units.
 * The {@link org.orekit.utils.units.Unit Unit} class provides useful
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} methods in case the callers
 * already use CCSDS units instead of the API SI units. The general-purpose
 * {@link org.orekit.utils.units.Unit Unit} class (without an 's') and the
 * CCSDS-specific {@link org.orekit.files.ccsds.definitions.Units Units} class
 * (with an 's') also provide some predefined units. These predefined units and the
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} conversion methods are indeed
 * what the parsers and writers use for the conversions.
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudeDeterminationSensor extends CommentsContainer {

    /** Sensor number. */
    private int sensorNumber;

    /** Sensor used. */
    private String sensorUsed;

    /** Number of noise elements for sensor. */
    private int nbSensorNoiseCovariance;

    /** Standard deviation of sensor noises for sensor. */
    private double[] sensorNoiseCovariance;

    /** Frequency of sensor data. */
    private double sensorFrequency;

    /** Simple constructor.
     */
    public AttitudeDeterminationSensor() {
        nbSensorNoiseCovariance = -1;
        sensorFrequency         = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNegative(sensorNumber, AttitudeDeterminationSensorKey.SENSOR_NUMBER.name());
        checkNotNull(sensorUsed, AttitudeDeterminationSensorKey.SENSOR_USED.name());
        if (nbSensorNoiseCovariance >= 0) {
            final int n = sensorNoiseCovariance == null ? 0 : sensorNoiseCovariance.length;
            if (nbSensorNoiseCovariance != n) {
                throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_ELEMENTS,
                                          nbSensorNoiseCovariance, n);
            }
        }
    }

    /** Get number of the sensor.
     * @return number of the sensor
     */
    public int getSensorNumber() {
        return sensorNumber;
    }

    /** Set number of the sensor.
     * @param sensorNumber number of the sensor
     */
    public void setSensorNumber(final int sensorNumber) {
        this.sensorNumber = sensorNumber;
    }

    /** Get sensor used.
     * @return sensor used
     */
    public String getSensorUsed() {
        return sensorUsed;
    }

    /** Set sensor used.
     * @param sensorUsed sensor used
     */
    public void setSensorUsed(final String sensorUsed) {
        this.sensorUsed = sensorUsed;
    }

    /** Get number of noise elements for sensor.
     * @return number of noise elements for sensor
     */
    public int getNbSensorNoiseCovariance() {
        return nbSensorNoiseCovariance;
    }

    /** Set number of noise elements for sensor.
     * @param n number of noise elements for sensor
     */
    public void setNbSensorNoiseCovariance(final int n) {
        nbSensorNoiseCovariance = n;
    }

    /** Get standard deviation of sensor noise for sensor.
     * @return standard deviation of sensor noise for sensor
     */
    public double[] getSensorNoiseCovariance() {
        return sensorNoiseCovariance == null ? null : sensorNoiseCovariance.clone();
    }

    /** Set standard deviation of sensor noise for sensor.
     * @param stddev standard deviation of sensor noise
     */
    public void setSensorNoiseCovariance(final double[] stddev) {
        if (stddev.length != nbSensorNoiseCovariance) {
            throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_ELEMENTS,
                                      nbSensorNoiseCovariance, stddev.length);
        }
        sensorNoiseCovariance = stddev.clone();
    }

    /** Get frequency of sensor data for sensor.
     * @return frequency of sensor data for sensor
     */
    public double getSensorFrequency() {
        return sensorFrequency;
    }

    /** Set frequency of sensor data for sensor.
     * @param frequency frequency of sensor data for sensor
     */
    public void setSensorFrequency(final double frequency) {
        sensorFrequency = frequency;
    }

}
