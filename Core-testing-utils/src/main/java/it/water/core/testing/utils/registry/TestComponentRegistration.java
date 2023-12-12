/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package it.water.core.testing.utils.registry;

import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.registry.model.ComponentConfigurationFactory;
import lombok.Getter;

import java.util.Properties;

public class TestComponentRegistration<T> implements ComponentRegistration<Object, Class<?>> {

    private T component;
    @Getter
    private Class<?> registrationClass;
    @Getter
    private Properties configuration;

    public TestComponentRegistration(Class<?> registrationClass, T component, Properties configuration) {
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
        return ComponentConfigurationFactory.createNewComponentPropertyFactory().fromGenericDictionary(this.configuration).build();
    }

    @Override
    public Class<?> getRegistration() {
        return registrationClass;
    }
}
