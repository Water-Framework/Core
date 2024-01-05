
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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @param <T> Component'sProperty key type
 *            Helper class that build component's properties.
 * @Author Aristide Cittadino
 */
public class ComponentConfigurationFactory<T> {
    private Map<T, Object> values;
    private int priority = 1;

    public ComponentConfigurationFactory() {
        values = new HashMap<>();
    }

    public ComponentConfigurationFactory<T> withProp(T name, Object value) {
        values.put(name, value);
        return this;
    }

    public ComponentConfigurationFactory<T> withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public ComponentConfigurationFactory<T> fromStringDictionary(Dictionary<String, Object> dictionary) {
        createDictionary(dictionary);
        return this;
    }

    public ComponentConfigurationFactory<T> fromGenericDictionary(Dictionary<Object, Object> dictionary) {
        createDictionary(dictionary);
        return this;
    }

    private ComponentConfigurationFactory<T> createDictionary(Dictionary<?, Object> dictionary) {
        Map<T, Object> map = new HashMap<>();
        Enumeration<?> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            T key = (T) keys.nextElement();
            map.put(key, dictionary.get(key));
        }
        values.putAll(map);
        return this;
    }

    public it.water.core.api.registry.ComponentConfiguration build() {
        return new PropertiesComponentConfiguration(priority, values);
    }

    public static <K> ComponentConfigurationFactory<K> createNewComponentPropertyFactory() {
        return new ComponentConfigurationFactory<>();
    }

}
