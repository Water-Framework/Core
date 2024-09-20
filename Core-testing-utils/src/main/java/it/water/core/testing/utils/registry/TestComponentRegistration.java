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
package it.water.core.testing.utils.registry;

import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import lombok.Getter;

public class TestComponentRegistration<T> implements ComponentRegistration<Object, Class<?>> {

    private T component;
    @Getter
    private Class<?> registrationClass;
    @Getter
    private ComponentConfiguration configuration;

    public TestComponentRegistration(Class<?> registrationClass, T component, ComponentConfiguration configuration) {
        this.component = component;
        this.registrationClass = registrationClass;
        this.configuration = configuration;
    }

    @Override
    public Object getComponent() {
        return component;
    }

    @Override
    public ComponentConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Class<?> getRegistration() {
        return registrationClass;
    }
}
