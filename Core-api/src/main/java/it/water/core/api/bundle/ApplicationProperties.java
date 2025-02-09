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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Autor Aristide Cittadino
 * This class implements the concept of Application Properties.
 * Every technology has its own method to load app props, this class is necessary to abstracts this logic for each technology.
 */
public interface ApplicationProperties {
    Pattern ENV_PATTERN = Pattern.compile(
            "\\$\\{env:([^:}]+)(:-([^}]+))?}"
    );

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
     * Resolve property value trying to load it from env variable or using the default value
     * property can be prop=value or prop=${ENV_VAR} or prop=${ENV_VAR:-default}
     * @param propertyValue
     * @return
     */
    default String resolvePropertyValue(String propertyValue) {
        if (propertyValue == null) {
            return null;
        }
        Matcher matcher = ENV_PATTERN.matcher(propertyValue);
        if (!matcher.find()) {
            return propertyValue;
        }
        StringBuffer sb = new StringBuffer();
        do {
            String varName = matcher.group(1);
            String defaultValue = matcher.group(3);
            String envValue = System.getenv(varName);
            if (envValue == null) {
                envValue = (defaultValue != null) ? defaultValue : "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(envValue));
        } while (matcher.find());
        matcher.appendTail(sb);
        return sb.toString();
    }

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
