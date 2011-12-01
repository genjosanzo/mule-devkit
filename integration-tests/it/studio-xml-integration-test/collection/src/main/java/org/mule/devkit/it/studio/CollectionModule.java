package org.mule.devkit.it.studio;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Optional;

import java.util.List;
import java.util.Map;

/**
 * Collection module
 *
 * @author MuleSoft, Inc.
 */
@Module(name = "collection")
@SuppressWarnings("unchecked")
public class CollectionModule {
    /**
     * Configurable strings
     */
    @Configurable
    @Optional
    private List<String> strings;

    /**
     * Configurable items
     */
    @Configurable
    @Optional
    private List items;

    /**
     * Configurable map of strings
     */
    @Configurable
    @Optional
    private Map<String, String> mapStrings;

    /**
     * Configurable list of strings
     */
    @Configurable
    @Optional
    private Map mapItems;

    /**
     * Method that accepts a List<String>
     *
     * @param strings a list of strings
     */
    @Processor
    public void listOfStrings(List<String> strings) {
    }

    /**
     * Method that accepts a Map<String, String>
     *
     * @param mapStrings a map of strings
     */
    @Processor
    public void mapOfStringString(Map<String, String> mapStrings) {
    }

    /**
     * Method that accepts a Map<String, Object>
     *
     * @param mapObjects a map of string-object
     */
    @Processor
    public void mapOfStringObject(Map<String, Object> mapObjects) {
    }

    /**
     * Method that accepts a raw Map
     *
     * @param properties a raw map
     */
    @Processor
    public void rawMap(Map properties) {
    }

    /**
     * Method that accepts a raw List
     *
     * @param list a list
     */
    @Processor
    public void rawList(List list) {
    }

    /**
     * Method that accepts a List<Map<String, Strin>>
     *
     * @param objects a list of maps
     */
    @Processor
    public void listOfMapOfStringString(List<Map<String, String>> objects) {
    }

    /**
     * Method that accepts a Map<String, List<String>>
     *
     * @param map a map of string-lists
     */
    @Processor
    public void mapOfStringListOfStrings(Map<String, List<String>> map) {
    }

    public void setStrings(List strings) {
        this.strings = strings;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public void setMapStrings(Map mapStrings) {
        this.mapStrings = mapStrings;
    }

    public void setMapItems(Map<String, String> mapItems) {
        this.mapItems = mapItems;
    }
}