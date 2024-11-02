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

import it.water.core.api.bundle.Runtime;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.service.rest.RestApiManager;
import it.water.core.api.service.rest.RestApiRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class BundleTest {
    @Mock
    ComponentRegistry componentRegistry;
    @Mock
    Runtime runtime;

    Map<Class<?>, List<Object>> components;

    BundleTestInitializer bundleTestInitializer;

    @BeforeEach
    void init() {
        components = new HashMap<>();

        Mockito.lenient().when(componentRegistry.findComponents(Mockito.any(), Mockito.any(ComponentFilter.class))).thenAnswer(new Answer<List<Object>>() {
            @Override
            public List<Object> answer(InvocationOnMock invocation) throws Throwable {
                return components.get(invocation.getArguments()[0]);
            }
        });

        Mockito.lenient().when(componentRegistry.findComponent(Mockito.any(), Mockito.any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if(RestApiManager.class.isAssignableFrom((Class)(invocation.getArguments()[0])))
                    return new FakeRestApiManager();
                if(RestApiRegistry.class.isAssignableFrom((Class)(invocation.getArguments()[0])))
                    return new FakeRestApiRegistry();
                return components.get(invocation.getArguments()[0]).get(0);
            }
        });

        Mockito.lenient().when(componentRegistry.registerComponent(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Class<?> componentClass = (Class) invocation.getArguments()[0];
                Object component = invocation.getArguments()[1];
                if (!components.containsKey(componentClass))
                    components.put(componentClass, new ArrayList<>());
                components.get(componentClass).add(component);
                return new TestRegistration(component);
            }
        });

        Mockito.lenient().when(runtime.getSecurityContext()).thenReturn(null);
        this.bundleTestInitializer = new BundleTestInitializer(componentRegistry, runtime);
        this.bundleTestInitializer.start();

    }

    @Test
    void test001_checkRegistryAndInitializer() {
        TestComponent t = this.componentRegistry.findComponent(TestComponent.class, null);
        Assertions.assertNotNull(t);
        Assertions.assertNotNull(t.getRegistry());
    }

    @Test
    void testRuntime(){
        WaterRuntime runtime = new WaterRuntime();
        SecurityContext sampleSecContext = new SecurityContext() {
            @Override
            public String getLoggedUsername() {
                return "";
            }

            @Override
            public boolean isLoggedIn() {
                return false;
            }

            @Override
            public boolean isAdmin() {
                return false;
            }

            @Override
            public long getLoggedEntityId() {
                return 0;
            }
        };
        Assertions.assertDoesNotThrow(() -> runtime.fillSecurityContext(sampleSecContext));
        Assertions.assertNotNull(runtime.getSecurityContext());
    }
}
