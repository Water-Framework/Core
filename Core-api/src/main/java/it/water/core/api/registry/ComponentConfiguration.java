
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

package it.water.core.api.registry;
import java.util.Dictionary;
import java.util.Properties;


/**
 * @Author Aristide Cittadino.
 * This interface represents the concept of bean configuration.
 * There'll be N implementation of this interface for Spring, Quarkus , OSGi and other frameworks...
 * Some framework may have this others not.
 */
public interface ComponentConfiguration {
    String COMPONENT_PRIORITY_PROPERTY = "it.water.component.priority";
    int getPriority();

    boolean isPrimary();
    Properties getConfiguration();
    Dictionary<String,Object> getConfigurationAsDictionary();
    void addProperty(String name,Object value);
    void removeProperty(String name);
    boolean hasProperty(String name);
}
