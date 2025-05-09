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
package org.orekit.propagation.analytical.gnss.data;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.gnss.RadioWave;

import java.util.function.Function;

/**
 * Container for data contained in a Beidou civilian navigation message.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
public class FieldBeidouCivilianNavigationMessage<T extends CalculusFieldElement<T>>
    extends FieldAbstractNavigationMessage<T, BeidouCivilianNavigationMessage> {

    /** Radio wave on which navigation signal is sent. */
    private final RadioWave radioWave;

    /** Change rate in semi-major axis (m/s). */
    private T aDot;

    /** Change rate in Δn₀. */
    private T deltaN0Dot;

    /** Issue of Data, Ephemeris. */
    private int iode;

    /** Issue of Data, Clock. */
    private int iodc;

    /** Inter Signal Delay for B1 CD. */
    private T iscB1CD;

    /** Inter Signal Delay for B1 CP. */
    private T iscB1CP;

    /** Inter Signal Delay for B2 AD. */
    private T iscB2AD;

    /** Signal In Space Accuracy Index (along track and across track). */
    private int sisaiOe;

    /** Signal In Space Accuracy Index (radial and clock). */
    private int sisaiOcb;

    /** Signal In Space Accuracy Index (clock drift accuracy). */
    private int sisaiOc1;

    /** Signal In Space Accuracy Index (clock drift rate accuracy). */
    private int sisaiOc2;

    /** Signal In Space Monitoring Accuracy Index. */
    private int sismai;

    /** Health. */
    private int health;

    /** Integrity flags. */
    private int integrityFlags;

    /** B1/B3 Group Delay Differential (s). */
    private T tgdB1Cp;

    /** B2 AP Group Delay Differential (s). */
    private T tgdB2ap;

    /** B2B_i / B3I Group Delay Differential (s). */
    private T tgdB2bI;

    /** Satellite type. */
    private BeidouSatelliteType satelliteType;

    /** Constructor from non-field instance.
     * @param field    field to which elements belong
     * @param original regular non-field instance
     */
    public FieldBeidouCivilianNavigationMessage(final Field<T> field, final BeidouCivilianNavigationMessage original) {
        super(field, original);
        this.radioWave = original.getRadioWave();
        setADot(field.getZero().newInstance(original.getADot()));
        setDeltaN0Dot(field.getZero().newInstance(original.getDeltaN0Dot()));
        setIODE(original.getIODE());
        setIODC(original.getIODC());
        setIscB1CD(field.getZero().newInstance(original.getIscB1CD()));
        setIscB1CP(field.getZero().newInstance(original.getIscB1CP()));
        setIscB2AD(field.getZero().newInstance(original.getIscB2AD()));
        setSisaiOe(original.getSisaiOe());
        setSisaiOcb(original.getSisaiOcb());
        setSisaiOc1(original.getSisaiOc1());
        setSisaiOc2(original.getSisaiOc2());
        setSismai(original.getSismai());
        setHealth(original.getHealth());
        setIntegrityFlags(original.getIntegrityFlags());
        setTgdB1Cp(field.getZero().newInstance(original.getTgdB1Cp()));
        setTgdB2ap(field.getZero().newInstance(original.getTgdB2ap()));
        setTgdB2bI(field.getZero().newInstance(original.getTgdB2bI()));
        setSatelliteType(original.getSatelliteType());
    }

    /** Constructor from different field instance.
     * @param <V> type of the old field elements
     * @param original regular non-field instance
     * @param converter for field elements
     */
    public <V extends CalculusFieldElement<V>> FieldBeidouCivilianNavigationMessage(final Function<V, T> converter,
                                                                                    final FieldBeidouCivilianNavigationMessage<V> original) {
        super(converter, original);
        this.radioWave = original.getRadioWave();
        setADot(converter.apply(original.getADot()));
        setDeltaN0Dot(converter.apply(original.getDeltaN0Dot()));
        setIODE(original.getIODE());
        setIODC(original.getIODC());
        setIscB1CD(converter.apply(original.getIscB1CD()));
        setIscB1CP(converter.apply(original.getIscB1CP()));
        setIscB2AD(converter.apply(original.getIscB2AD()));
        setSisaiOe(original.getSisaiOe());
        setSisaiOcb(original.getSisaiOcb());
        setSisaiOc1(original.getSisaiOc1());
        setSisaiOc2(original.getSisaiOc2());
        setSismai(original.getSismai());
        setHealth(original.getHealth());
        setIntegrityFlags(original.getIntegrityFlags());
        setTgdB1Cp(converter.apply(original.getTgdB1Cp()));
        setTgdB2ap(converter.apply(original.getTgdB2ap()));
        setTgdB2bI(converter.apply(original.getTgdB2bI()));
        setSatelliteType(original.getSatelliteType());
    }

    /** {@inheritDoc} */
    @Override
    public BeidouCivilianNavigationMessage toNonField() {
        return new BeidouCivilianNavigationMessage(this);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <U extends CalculusFieldElement<U>, G extends FieldGnssOrbitalElements<U, BeidouCivilianNavigationMessage>>
        G changeField(final Function<T, U> converter) {
        return (G) new FieldBeidouCivilianNavigationMessage<>(converter, this);
    }

    /**
     * Getter for radio wave.
     * @return radio wave on which navigation signal is sent
     */
    public RadioWave getRadioWave() {
        return radioWave;
    }

    /**
     * Getter for the change rate in semi-major axis.
     * @return the change rate in semi-major axis
     */
    public T getADot() {
        return aDot;
    }

    /**
     * Setter for the change rate in semi-major axis.
     * @param value the change rate in semi-major axis
     */
    public void setADot(final T value) {
        this.aDot = value;
    }

    /**
     * Getter for change rate in Δn₀.
     * @return change rate in Δn₀
     */
    public T getDeltaN0Dot() {
        return deltaN0Dot;
    }

    /**
     * Setter for change rate in Δn₀.
     * @param deltaN0Dot change rate in Δn₀
     */
    public void setDeltaN0Dot(final T deltaN0Dot) {
        this.deltaN0Dot = deltaN0Dot;
    }

    /**
     * Getter for the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /**
     * Setter for the Issue of Data Ephemeris.
     * @param value the IODE to set
     */
    public void setIODE(final int value) {
        this.iode = value;
    }

    /**
     * Getter for the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /**
     * Setter for the Issue of Data Clock.
     * @param value the IODC to set
     */
    public void setIODC(final int value) {
        this.iodc = value;
    }

    /**
     * Getter for inter Signal Delay for B1 CD.
     * @return inter signal delay
     */
    public T getIscB1CD() {
        return iscB1CD;
    }

    /**
     * Setter for inter Signal Delay for B1 CD.
     * @param delay delay to set
     */
    public void setIscB1CD(final T delay) {
        this.iscB1CD = delay;
    }

    /**
     * Getter for inter Signal Delay for B2 AD.
     * @return inter signal delay
     */
    public T getIscB2AD() {
        return iscB2AD;
    }

    /**
     * Setter for inter Signal Delay for B2 AD.
     * @param delay delay to set
     */
    public void setIscB2AD(final T delay) {
        this.iscB2AD = delay;
    }

    /**
     * Getter for inter Signal Delay for B1 CP.
     * @return inter signal delay
     */
    public T getIscB1CP() {
        return iscB1CP;
    }

    /**
     * Setter for inter Signal Delay for B1 CP.
     * @param delay delay to set
     */
    public void setIscB1CP(final T delay) {
        this.iscB1CP = delay;
    }

    /**
     * Getter for Signal In Space Accuracy Index (along track and across track).
     * @return Signal In Space Accuracy Index (along track and across track)
     */
    public int getSisaiOe() {
        return sisaiOe;
    }

    /**
     * Setter for Signal In Space Accuracy Index (along track and across track).
     * @param sisaiOe Signal In Space Accuracy Index (along track and across track)
     */
    public void setSisaiOe(final int sisaiOe) {
        this.sisaiOe = sisaiOe;
    }

    /**
     * Getter for Signal In Space Accuracy Index (radial and clock).
     * @return Signal In Space Accuracy Index (radial and clock)
     */
    public int getSisaiOcb() {
        return sisaiOcb;
    }

    /**
     * Setter for Signal In Space Accuracy Index (radial and clock).
     * @param sisaiOcb Signal In Space Accuracy Index (radial and clock)
     */
    public void setSisaiOcb(final int sisaiOcb) {
        this.sisaiOcb = sisaiOcb;
    }

    /**
     * Getter for Signal In Space Accuracy Index (clock drift accuracy).
     * @return Signal In Space Accuracy Index (clock drift accuracy)
     */
    public int getSisaiOc1() {
        return sisaiOc1;
    }

    /**
     * Setter for Signal In Space Accuracy Index (clock drift accuracy).
     * @param sisaiOc1 Signal In Space Accuracy Index (clock drift accuracy)
     */
    public void setSisaiOc1(final int sisaiOc1) {
        this.sisaiOc1 = sisaiOc1;
    }

    /**
     * Getter for Signal In Space Accuracy Index (clock drift rate accuracy).
     * @return Signal In Space Accuracy Index (clock drift rate accuracy)
     */
    public int getSisaiOc2() {
        return sisaiOc2;
    }

    /**
     * Setter for Signal In Space Accuracy Index (clock drift rate accuracy).
     * @param sisaiOc2 Signal In Space Accuracy Index (clock drift rate accuracy)
     */
    public void setSisaiOc2(final int sisaiOc2) {
        this.sisaiOc2 = sisaiOc2;
    }

    /**
     * Getter for Signal In Space Monitoring Accuracy Index.
     * @return Signal In Space Monitoring Accuracy Index
     */
    public int getSismai() {
        return sismai;
    }

    /**
     * Setter for Signal In Space Monitoring Accuracy Index.
     * @param sismai Signal In Space Monitoring Accuracy Index
     */
    public void setSismai(final int sismai) {
        this.sismai = sismai;
    }

    /**
     * Getter for health.
     * @return health
     */
    public int getHealth() {
        return health;
    }

    /**
     * Setter for health.
     * @param health health
     */
    public void setHealth(final int health) {
        this.health = health;
    }

    /**
     * Getter for B1C integrity flags.
     * @return B1C integrity flags
     */
    public int getIntegrityFlags() {
        return integrityFlags;
    }

    /**
     * Setter for B1C integrity flags.
     * @param integrityFlags integrity flags
     */
    public void setIntegrityFlags(final int integrityFlags) {
        this.integrityFlags = integrityFlags;
    }

    /**
     * Getter for B1/B3 Group Delay Differential (s).
     * @return B1/B3 Group Delay Differential (s)
     */
    public T getTgdB1Cp() {
        return tgdB1Cp;
    }

    /**
     * Setter for B1/B3 Group Delay Differential (s).
     * @param tgdB1Cp B1/B3 Group Delay Differential (s)
     */
    public void setTgdB1Cp(final T tgdB1Cp) {
        this.tgdB1Cp = tgdB1Cp;
    }

    /**
     * Getter for B2 AP Group Delay Differential (s).
     * @return B2 AP Group Delay Differential (s)
     */
    public T getTgdB2ap() {
        return tgdB2ap;
    }

    /**
     * Setter for B2 AP Group Delay Differential (s).
     * @param tgdB2ap B2 AP Group Delay Differential (s)
     */
    public void setTgdB2ap(final T tgdB2ap) {
        this.tgdB2ap = tgdB2ap;
    }

    /**
     * Getter for B2B_i / B3I Group Delay Differential (s).
     * @return B2B_i / B3I Group Delay Differential (s)
     */
    public T getTgdB2bI() {
        return tgdB2bI;
    }

    /**
     * Setter for B2B_i / B3I Group Delay Differential (s).
     * @param tgdB2bI B2B_i / B3I Group Delay Differential (s)
     */
    public void setTgdB2bI(final T tgdB2bI) {
        this.tgdB2bI = tgdB2bI;
    }

    /**
     * Getter for satellite type.
     * @return satellite type
     */
    public BeidouSatelliteType getSatelliteType() {
        return satelliteType;
    }

    /**
     * Setter for satellite type.
     * @param satelliteType satellite type
     */
    public void setSatelliteType(final BeidouSatelliteType satelliteType) {
        this.satelliteType = satelliteType;
    }

}
