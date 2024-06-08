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
package it.water.core.testing.utils.bundle;

import it.water.core.api.bundle.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @Author Aristide Cittadino
 * OSGi Application Properties, registered manually as component at startup
 */
public class TestApplicationProperties implements ApplicationProperties {
    private static Logger logger = LoggerFactory.getLogger(TestApplicationProperties.class);
    private static final String APPLICATION_TEST_PROPS = "src/test/resources/it.water.application.properties";
    private Properties properties;

    @Override
    public void setup() {
        this.properties = new Properties();
        File cfgPath = new File(APPLICATION_TEST_PROPS);
        loadProperties(cfgPath);
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public Object containsKey(String key) {
        return properties.containsKey(key);
    }

    /**
     * Just for test purpose
     * @param key
     * @param value
     */
    public void override(String key, Object value) {
        properties.put(key, value);
    }

    @Override
    public void loadProperties(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Properties tmp = new Properties();
            tmp.load(fis);
            loadProperties(tmp);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void loadProperties(Properties props) {
        this.properties.putAll(props);
    }

    @Override
    public void unloadProperties(File file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unloadProperties(Properties props) {
        props.keySet().forEach(key -> this.properties.remove(key));
    }

}
