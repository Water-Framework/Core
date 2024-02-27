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
package it.water.core.bundle;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.registry.model.ComponentConfigurationFactory;

/**
 * @param <T>
 * @param <K>
 * @Author Aristide Cittadino
 * This class should be used when a new technology wants to embed Framework.
 * For example the Spring or OSGi basic implementations must provide a concretization of RuntimeInitializer.
 * This because this initializer will setup the component registry and the runtime.
 */
public abstract class RuntimeInitializer<T, K> extends ApplicationInitializer<T, K> {

    @Override
    protected void initializeFrameworkComponents() {
        this.initializeFrameworkComponents(true);
    }

    /**
     * Method used for register inside the current component registry all @FrameworkComponents.
     *
     * @param registerRegistry
     */
    protected void initializeFrameworkComponents(boolean registerRegistry) {
        if (registerRegistry)
            registerComponentRegistryAsComponent();
        super.initializeFrameworkComponents();
    }

    /**
     * This method can be overriden.
     * It basically assumes that the initializer has a specific instance of the component registry created manually.
     * Using this instance , the initializer can use itself to register to the final framework as a component.
     * Note: please override this method only in case this uses case does not fit you framework.
     */
    protected void registerComponentRegistryAsComponent() {
        ComponentRegistry specificInstance = getComponentRegistry();
        specificInstance.registerComponent(ComponentRegistry.class, specificInstance, ComponentConfigurationFactory.createNewComponentPropertyFactory().build());
    }
}
