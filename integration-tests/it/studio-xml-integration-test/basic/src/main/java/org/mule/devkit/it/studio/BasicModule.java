package org.mule.devkit.it.studio;

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;

/**
 * Basic module
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "basic")
public class BasicModule {

    /**
     * Passthru char
     *
     * @param value Value to passthru
     * @return The same char
     */
    @Processor
    public char passthruChar(char value) {
        return value;
    }

    /**
     * Passthru string
     *
     * @param value Value to passthru
     * @return The same string
     */
    @Processor
    public String passthruString(String value) {
        return value;
    }

    /**
     * Passthru float
     *
     * @param value Value to passthru
     * @return The same float
     */
    @Processor
    public float passthruFloat(float value) {
        return value;
    }

    /**
     * Passthru boolean
     *
     * @param value Value to passthru
     * @return The same boolean
     */
    @Processor
    public boolean passthruBoolean(boolean value) {
        return value;
    }

    /**
     * Passthru integer
     *
     * @param value Value to passthru
     * @return The same integer
     */
    @Processor
    public int passthruInteger(int value) {
        return value;
    }

    /**
     * Passthru long
     *
     * @param value Value to passthru
     * @return The same long
     */
    @Processor
    public long passthruLong(long value) {
        return value;
    }

    /**
     * Passthru complex float
     *
     * @param value Value to passthru
     * @return The same complex float
     */
    @Processor
    public Float passthruComplexFloat(Float value) {
        return value;
    }

    /**
     * Passthru complex boolean
     *
     * @param value Value to passthru
     * @return The same complex boolean
     */
    @Processor
    public Boolean passthruComplexBoolean(Boolean value) {
        return value;
    }

    /**
     * Passthru complex integer
     *
     * @param value Value to passthru
     * @return The same complex integer
     */
    @Processor
    public Integer passthruComplexInteger(Integer value) {
        return value;
    }

    /**
     * Passthru complex long
     *
     * @param value Value to passthru
     * @return The same complex long
     */
    @Processor
    public Long passthruComplexLong(Long value) {
        return value;
    }

    public enum Mode {
        In,
        Out
    }

    /**
     * Passthru mode enum
     *
     * @param mode Value to passthru
     * @return The same cmode enum
     */
    @Processor
    public String passthruEnum(Mode mode) {
        return mode.name();
    }

    /**
     * Passthru complex object
     *
     * @param myComplexObject Value to passthru
     * @return The same complex object
     */
    @Processor
    public String passthruComplexRef(MyComplexObject myComplexObject) {
        return myComplexObject.getValue();
    }
}