/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.water.core.api.bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

/**
 * @Autor Aristide Cittadino
 * This class implements the concept of Application Properties.
 * Every technology has its own method to load app props, this class is necessary to abstracts this logic for each technology.
 */
public interface ApplicationProperties {

    Logger log = LoggerFactory.getLogger(ApplicationProperties.class);

    /**
     * Method which should be run at application startup in order to load all properties
     */
    void setup();

    /**
     * @return current property with the specified key
     */
    Object getProperty(String key);

    /**
     * @return true if property with the specified key exists
     */
    boolean containsKey(String key);

    /**
     * Adds properties contained inside file into current properties
     *
     * @param file
     */
    void loadProperties(File file);

    /**
     * Adds properties contained inside file into current properties
     *
     * @param props
     */
    void loadProperties(Properties props);

    /**
     * Removes properties matching those inside the specified file
     *
     * @param file
     */
    void unloadProperties(File file);

    /**
     * Removes properties matching those inside the specified file
     *
     * @param props
     */
    void unloadProperties(Properties props);

    /**
     * @param propName     property name
     * @param defaultValue default value
     * @return
     */
    default String getPropertyOrDefault(String propName, String defaultValue) {
        try {
            if (this.getProperty(propName) != null) {
                return String.valueOf(this.getProperty(propName));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * @param propName     property name
     * @param defaultValue default value
     * @return
     */
    default long getPropertyOrDefault(String propName, long defaultValue) {
        try {
            if (this.getProperty(propName) != null) {
                return Long.valueOf(String.valueOf(this.getProperty(propName)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return defaultValue;
    }

    /**
     * @param propName     property name
     * @param defaultValue default value
     * @return
     */
    default boolean getPropertyOrDefault(String propName, boolean defaultValue) {
        try {
            if (this.getProperty(propName) != null) {
                return Boolean.parseBoolean(String.valueOf(this.getProperty(propName)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return defaultValue;
    }
}
