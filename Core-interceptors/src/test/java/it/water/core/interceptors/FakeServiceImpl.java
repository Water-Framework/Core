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
package it.water.core.interceptors;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;

@FrameworkComponent
public class FakeServiceImpl implements FakeService {

    //used to test injection
    //Forcing public to find the field through reflection
    @Inject
    private ComponentRegistry registryInjected;

    //Injection will fail because no setter has been defined
    @Inject
    private ComponentRegistry registryNotInjected;


    //Used to test injection for the component injector
    //and testing method interceptors based on annotation
    //Forcing public to find the method through reflection
    @TestMethodAnnotation
    public void invokeMethod() {
        //fake invocation
    }

    public ComponentRegistry getRegistryInjected() {
        return registryInjected;
    }

    //Used for injection
    public void setRegistryInjected(ComponentRegistry registryInjected) {
        this.registryInjected = registryInjected;
    }

    public ComponentRegistry getRegistryNotInjected() {
        return registryNotInjected;
    }
}
