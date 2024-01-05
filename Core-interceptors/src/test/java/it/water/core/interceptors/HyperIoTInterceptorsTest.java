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
package it.water.core.interceptors;

import it.water.core.api.interceptors.AfterMethodFieldInterceptor;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.implementation.WaterComponentsInjector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class InterceptorsTest {
    private static Logger log = LoggerFactory.getLogger(InterceptorsTest.class);
    @Mock
    private ComponentRegistry registry;

    private WaterComponentsInjector injector;

    private FakeInterceptor fakeProxy;

    private FakeService fakeService;

    @BeforeEach
    void init() {
        // Returning always the registry since no other components is required for injection
        Mockito.lenient().when(registry.findComponents(Mockito.any(), Mockito.any())).thenAnswer(new Answer<List<Object>>() {
            @Override
            public List<Object> answer(InvocationOnMock invocation) throws Throwable {
                Class<?> classType = (Class<?>) invocation.getArguments()[0];
                String name = classType.getName();
                if (name.endsWith("ComponentRegistry"))
                    return Collections.singletonList(registry);
                else if (name.endsWith("ComponentsInjector"))
                    return Collections.singletonList(injector);
                else if (name.endsWith("BeforeMethodInterceptor")) {
                    return Collections.singletonList(new TestBeforeMethodInterceptor());
                } else if (name.endsWith("AfterMethodInterceptor")) {
                    return Collections.singletonList(new TestBeforeMethodInterceptor());
                } else
                    return Collections.singletonList(this);
            }
        });

        Mockito.lenient().when(registry.findComponent(Mockito.any(), Mockito.any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Class<?> classType = (Class<?>) invocation.getArguments()[0];
                String name = classType.getName();
                if (name.endsWith("ComponentRegistry"))
                    return registry;
                else if (name.endsWith("ComponentsInjector"))
                    return injector;
                else if (name.endsWith("BeforeMethodInterceptor"))
                    return new TestBeforeMethodInterceptor();
                else if (name.endsWith("AfterMethodInterceptor")) {
                    return Collections.singletonList(new TestBeforeMethodInterceptor());
                } else
                    return this;
            }
        });


        Mockito.lenient().when(registry.registerComponent(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Class<?> classType = (Class<?>) invocation.getArguments()[0];
                String name = classType.getName();
                if (name.endsWith("ComponentRegistry"))
                    return new FakeComponentRegistration(registry);
                else if (name.endsWith("ComponentsInjector"))
                    return new FakeComponentRegistration(injector);
                else if (name.endsWith("BeforeMethodInterceptor"))
                    return new FakeComponentRegistration(new TestBeforeMethodInterceptor());
                else if (name.endsWith("AfterMethodInterceptor")) {
                    return Collections.singletonList(new TestBeforeMethodInterceptor());
                } else
                    return new FakeComponentRegistration(this);

            }
        });
        this.injector = new WaterComponentsInjector();
        injector.setComponentRegistry(registry);
        fakeProxy = new FakeInterceptor(registry);
        fakeService = new FakeServiceImpl();
    }

    //Forcing registry to be injected dinamically every time
    @AfterEach
    void nullComponentRegistry() {
        this.fakeService.setRegistryInjected(null);
    }

    @Test
    void componentProxyTest() throws NoSuchMethodException, NoSuchFieldException {
        Method m = fakeService.getClass().getMethod("invokeMethod");
        //invoking intercept before method
        fakeProxy.interceptMethod(fakeService, m, null, null);
        Assertions.assertNotNull(this.fakeService.getRegistryInjected());
        Assertions.assertNull(this.fakeService.getRegistryNotInjected());
        fakeProxy.setService(fakeService);
        Assertions.assertEquals(FakeServiceImpl.class, fakeProxy.getOriginalConcreteClass());
        Assertions.assertNotNull(fakeProxy.getService());
    }

    @Test
    void injectorFail() throws NoSuchMethodException {
        Method m = fakeService.getClass().getMethod("invokeMethod");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            injector.interceptMethod(fakeService, m, null, null);
        });

        final AfterMethodFieldInterceptor afterMethodFieldInterceptor = new AfterMethodFieldInterceptor<>() {
            @Override
            public <S extends Service> void interceptMethod(S destination, Method m, List<Field> annotatedFields, Object[] args, Annotation annotation) {
                //do nothing
            }
        };
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            afterMethodFieldInterceptor.interceptMethod(null, (Method) null, (Object[]) null, null, null);
        });
    }

    void testMethodInterceptors() {

    }
}
