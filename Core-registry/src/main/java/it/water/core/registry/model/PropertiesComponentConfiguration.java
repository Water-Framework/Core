
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

package it.water.core.registry.model;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * @Author Aristide Cittadino
 */
public class PropertiesComponentConfiguration implements it.water.core.api.registry.ComponentConfiguration {
    private static final Logger log = LoggerFactory.getLogger(PropertiesComponentConfiguration.class);
    private Properties props;
    @Getter
    private int priority;
    @Getter
    private boolean primary;

    public PropertiesComponentConfiguration() {
        this.initProps();
    }

    public PropertiesComponentConfiguration(File file) throws IOException {
        this.initProps();
        try (FileReader fr = new FileReader(file)) {
            this.props.load(fr);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public PropertiesComponentConfiguration(int priority,boolean primary, Map<?, Object> props) {
        this.initProps();
        this.priority = priority;
        this.primary = primary;
        this.props.putAll(props);
    }

    private void initProps() {
        this.props = new Properties();
    }

    @Override
    public Properties getConfiguration() {
        Properties copy = new Properties();
        copy.putAll(props);
        return copy;
    }

    @Override
    public Dictionary<String, Object> getConfigurationAsDictionary() {
        Map<String, Object> map = new HashMap<>();
        this.props.forEach((name, val) -> map.put(name.toString(), val));
        return new Hashtable<>(map);
    }

    @Override
    public void addProperty(String name, Object value) {
        props.computeIfAbsent(name, key -> value);
    }

    @Override
    public void removeProperty(String name) {
        if (props.containsKey(name)) props.remove(name);
    }

    @Override
    public boolean hasProperty(String name) {
        return props.containsKey(name);
    }
}
