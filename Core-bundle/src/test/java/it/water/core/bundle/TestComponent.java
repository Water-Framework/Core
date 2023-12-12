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
package it.water.core.bundle;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;

/**
 * Testing component registration
 */
@FrameworkComponent(services = TestComponent.class)
public class TestComponent {

    //testing startup injection
    //not testing runtime injection since this component is not part of this bundle
    @Inject(injectOnceAtStartup = true)
    ComponentRegistry registry;

    public ComponentRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(ComponentRegistry registry) {
        this.registry = registry;
    }
}
